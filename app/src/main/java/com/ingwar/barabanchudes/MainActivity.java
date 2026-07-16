package com.ingwar.barabanchudes;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();
        setContentView(new GameView());
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private final class GameView extends View {
        private static final float WORLD_W = 896f;
        private static final float WORLD_H = 527f;

        private static final int WAIT_SPIN = 0;
        private static final int SPINNING = 1;
        private static final int WAIT_LETTER = 2;
        private static final int PRIZE_CHOICE = 3;
        private static final int ROUND_OVER = 4;

        private static final String ALPHABET = "АБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ";
        private static final int BOARD_SLOTS = 14;

        private final Paint paint = new Paint();
        private final Random random = new Random();
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final List<WordEntry> words = new ArrayList<>();
        private final Set<Character> usedLetters = new HashSet<>();

        private final Bitmap stage;
        private final Bitmap hostIdle;
        private final Bitmap hostTalk;
        private final Bitmap[] wheelFrames = new Bitmap[16];

        private final String[] names = {"Первый игрок", "Сникерс", "Снегирь"};
        private final int[] scores = new int[3];

        private final String[] frameSectors = {
                "10", "ПОДАРОК", "20", "x4",
                "5", "x2", "15", "БАНКРОТ",
                "20", "+", "5", "ПОДАРОК",
                "10", "25", "15", "ПРИЗ"
        };

        private final RectF spinButton = new RectF(20, 448, 112, 480);
        private final RectF wordButton = new RectF(114, 448, 211, 480);
        private final RectF prizeButton = new RectF(20, 448, 112, 480);
        private final RectF playButton = new RectF(114, 448, 211, 480);
        private final RectF wheelRect = new RectF(278, 235, 502, 421);

        private String category = "";
        private String answer = "";
        private boolean[] opened = new boolean[0];
        private int currentPlayer = 0;
        private int state = WAIT_SPIN;
        private int wheelFrame = 0;
        private int sectorPoints = 0;
        private int spinStepsLeft = 0;
        private long nextFrameAt = 0L;
        private String hostText = "";
        private boolean hostTalking = false;
        private boolean alive = true;

        GameView() {
            super(MainActivity.this);
            setKeepScreenOn(true);
            setFocusable(true);
            paint.setAntiAlias(false);
            paint.setFilterBitmap(false);
            paint.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL));

            stage = BitmapFactory.decodeResource(getResources(), R.drawable.stage_background);
            hostIdle = BitmapFactory.decodeResource(getResources(), R.drawable.host_idle);
            hostTalk = BitmapFactory.decodeResource(getResources(), R.drawable.host_talk);
            int[] ids = {
                    R.drawable.wheel_00, R.drawable.wheel_01, R.drawable.wheel_02, R.drawable.wheel_03,
                    R.drawable.wheel_04, R.drawable.wheel_05, R.drawable.wheel_06, R.drawable.wheel_07,
                    R.drawable.wheel_08, R.drawable.wheel_09, R.drawable.wheel_10, R.drawable.wheel_11,
                    R.drawable.wheel_12, R.drawable.wheel_13, R.drawable.wheel_14, R.drawable.wheel_15
            };
            for (int i = 0; i < ids.length; i++) {
                wheelFrames[i] = BitmapFactory.decodeResource(getResources(), ids[i]);
            }

            loadWords();
            newGame();
        }

        private void loadWords() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    getAssets().open("base.dat"), Charset.forName("windows-1251")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    int split = line.lastIndexOf(' ');
                    if (split <= 0 || split >= line.length() - 1) continue;
                    words.add(new WordEntry(line.substring(0, split), line.substring(split + 1)));
                }
            } catch (Exception e) {
                words.add(new WordEntry("Компьютеры", "ВИДЕОКАРТА"));
            }
        }

        private void newGame() {
            handler.removeCallbacksAndMessages(null);
            for (int i = 0; i < scores.length; i++) scores[i] = 0;
            currentPlayer = 0;
            wheelFrame = random.nextInt(wheelFrames.length);
            newRound();
        }

        private void newRound() {
            WordEntry entry = words.get(random.nextInt(words.size()));
            category = entry.category;
            answer = entry.answer.toUpperCase(new Locale("ru"));
            opened = new boolean[answer.length()];
            usedLetters.clear();
            currentPlayer = 0;
            state = WAIT_SPIN;
            hostSay("Первый игрок, вращайте\nбарабан!");
            invalidate();
        }

        private void hostSay(String text) {
            hostText = text;
            hostTalking = true;
            invalidate();
            handler.postDelayed(() -> {
                hostTalking = false;
                invalidate();
            }, 520);
        }

        private void spinWheel() {
            if (state != WAIT_SPIN || !alive) return;
            state = SPINNING;
            spinStepsLeft = 34 + random.nextInt(25);
            nextFrameAt = System.currentTimeMillis();
            hostSay(names[currentPlayer] + " вращает барабан!");
            handler.post(spinTick);
        }

        private final Runnable spinTick = new Runnable() {
            @Override
            public void run() {
                if (!alive || state != SPINNING) return;
                long now = System.currentTimeMillis();
                if (now < nextFrameAt) {
                    handler.postDelayed(this, Math.max(1L, nextFrameAt - now));
                    return;
                }

                wheelFrame = (wheelFrame + 1) % wheelFrames.length;
                spinStepsLeft--;
                invalidate();

                if (spinStepsLeft <= 0) {
                    finishSpin();
                    return;
                }

                int completed = 60 - Math.min(60, spinStepsLeft);
                long delay = 35L + (long) completed * completed / 18L;
                if (spinStepsLeft < 8) delay += (8L - spinStepsLeft) * 30L;
                nextFrameAt = now + Math.min(delay, 260L);
                handler.postDelayed(this, Math.min(delay, 260L));
            }
        };

        private void finishSpin() {
            String sector = frameSectors[wheelFrame];
            switch (sector) {
                case "БАНКРОТ":
                    scores[currentPlayer] = 0;
                    hostSay("Банкрот! Все очки\nсгорели!");
                    handler.postDelayed(this::nextTurn, 1300);
                    break;
                case "+":
                    int revealed = revealRandomLetter();
                    hostSay(revealed > 0 ? "Откройте любую букву!" : "Все буквы уже открыты!");
                    if (isSolved()) {
                        finishRound(names[currentPlayer] + " победил!");
                    } else {
                        state = WAIT_SPIN;
                        scheduleBot();
                    }
                    break;
                case "x2":
                    scores[currentPlayer] *= 2;
                    state = WAIT_SPIN;
                    hostSay("Ваши очки удваиваются!");
                    scheduleBot();
                    break;
                case "x4":
                    scores[currentPlayer] *= 4;
                    state = WAIT_SPIN;
                    hostSay("Ваши очки умножаются\nна четыре!");
                    scheduleBot();
                    break;
                case "ПРИЗ":
                case "ПОДАРОК":
                    if (currentPlayer == 0) {
                        state = PRIZE_CHOICE;
                        hostSay("Приз или играем дальше?");
                    } else {
                        scores[currentPlayer] += 25;
                        state = WAIT_SPIN;
                        hostSay(names[currentPlayer] + " выбирает игру!");
                        scheduleBot();
                    }
                    break;
                default:
                    sectorPoints = Integer.parseInt(sector);
                    state = WAIT_LETTER;
                    hostSay("У вас " + sectorPoints + " очков!\nНазовите букву.");
                    scheduleBot();
                    break;
            }
            invalidate();
        }

        private void choosePrize() {
            scores[currentPlayer] += 100;
            state = WAIT_SPIN;
            hostSay("Ваш приз — 100 очков!");
            invalidate();
        }

        private void keepPlaying() {
            state = WAIT_LETTER;
            sectorPoints = 25;
            hostSay("Играем! Назовите букву.");
            invalidate();
        }

        private void processLetter(char letter) {
            if (state != WAIT_LETTER) return;
            letter = Character.toUpperCase(letter);
            if (usedLetters.contains(letter)) {
                hostSay("Эту букву уже называли!");
                handler.postDelayed(this::nextTurn, 1100);
                return;
            }
            usedLetters.add(letter);

            int count = 0;
            for (int i = 0; i < answer.length(); i++) {
                if (answer.charAt(i) == letter && !opened[i]) {
                    opened[i] = true;
                    count++;
                }
            }

            if (count > 0) {
                scores[currentPlayer] += sectorPoints * count;
                if (isSolved()) {
                    finishRound(names[currentPlayer] + " угадал слово!");
                } else {
                    state = WAIT_SPIN;
                    hostSay(count == 1 ? "Есть такая буква!" : "Есть такие буквы!");
                    scheduleBot();
                }
            } else {
                hostSay("Нет такой буквы!");
                handler.postDelayed(this::nextTurn, 1050);
            }
            invalidate();
        }

        private void guessWord() {
            if (currentPlayer != 0 || (state != WAIT_SPIN && state != WAIT_LETTER)) return;
            EditText input = new EditText(MainActivity.this);
            input.setSingleLine(true);
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
            input.setTextSize(22);
            input.setHint("Введите слово");

            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Сказать слово")
                    .setView(input)
                    .setNegativeButton("Отмена", null)
                    .setPositiveButton("Сказать", null)
                    .create();
            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String value = input.getText().toString().trim().toUpperCase(new Locale("ru"));
                if (value.isEmpty()) {
                    Toast.makeText(MainActivity.this, "Введите слово", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                if (normalizeWord(value).equals(normalizeWord(answer))) {
                    for (int i = 0; i < opened.length; i++) opened[i] = true;
                    scores[currentPlayer] += 100;
                    finishRound("Вы угадали слово!");
                } else {
                    hostSay("Неправильно!");
                    handler.postDelayed(this::nextTurn, 1000);
                }
            }));
            dialog.show();
        }

        private String normalizeWord(String value) {
            return value.replace('Ё', 'Е').replace(" ", "");
        }

        private int revealRandomLetter() {
            List<Character> hidden = new ArrayList<>();
            for (int i = 0; i < answer.length(); i++) {
                char c = answer.charAt(i);
                if (!opened[i] && !hidden.contains(c)) hidden.add(c);
            }
            if (hidden.isEmpty()) return 0;
            char selected = hidden.get(random.nextInt(hidden.size()));
            usedLetters.add(selected);
            int count = 0;
            for (int i = 0; i < answer.length(); i++) {
                if (answer.charAt(i) == selected && !opened[i]) {
                    opened[i] = true;
                    count++;
                }
            }
            return count;
        }

        private boolean isSolved() {
            for (boolean item : opened) if (!item) return false;
            return true;
        }

        private void finishRound(String text) {
            state = ROUND_OVER;
            hostSay(text + "\nКоснитесь барабана.");
            invalidate();
        }

        private void nextTurn() {
            if (state == ROUND_OVER || !alive) return;
            currentPlayer = (currentPlayer + 1) % names.length;
            state = WAIT_SPIN;
            hostSay(names[currentPlayer] + ", вращайте\nбарабан!");
            invalidate();
            scheduleBot();
        }

        private void scheduleBot() {
            if (currentPlayer == 0 || state == ROUND_OVER || !alive) return;
            if (state == WAIT_SPIN) {
                handler.postDelayed(this::spinWheel, 850);
            } else if (state == WAIT_LETTER) {
                handler.postDelayed(this::botLetter, 850);
            }
        }

        private void botLetter() {
            if (currentPlayer == 0 || state != WAIT_LETTER) return;
            List<Character> hidden = new ArrayList<>();
            for (int i = 0; i < answer.length(); i++) {
                char c = answer.charAt(i);
                if (!opened[i] && !hidden.contains(c)) hidden.add(c);
            }
            char choice;
            if (!hidden.isEmpty() && random.nextInt(100) < 62) {
                choice = hidden.get(random.nextInt(hidden.size()));
            } else {
                do {
                    choice = ALPHABET.charAt(random.nextInt(ALPHABET.length()));
                } while (usedLetters.contains(choice) && usedLetters.size() < ALPHABET.length());
            }
            final char finalChoice = choice;
            hostSay(names[currentPlayer] + ": буква «" + choice + "»!");
            handler.postDelayed(() -> processLetter(finalChoice), 750);
        }

        @Override
        protected void onDetachedFromWindow() {
            alive = false;
            handler.removeCallbacksAndMessages(null);
            super.onDetachedFromWindow();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float scale = Math.min(getWidth() / WORLD_W, getHeight() / WORLD_H);
            float dx = (getWidth() - WORLD_W * scale) / 2f;
            float dy = (getHeight() - WORLD_H * scale) / 2f;
            canvas.drawColor(Color.BLACK);
            canvas.save();
            canvas.translate(dx, dy);
            canvas.scale(scale, scale);
            drawOriginalScene(canvas);
            canvas.restore();
        }

        private void drawOriginalScene(Canvas canvas) {
            canvas.drawBitmap(stage, 0, 0, paint);
            drawBoard(canvas);
            drawLabels(canvas);
            canvas.drawBitmap(wheelFrames[wheelFrame], wheelRect.left, wheelRect.top, paint);
            Bitmap host = hostTalking ? hostTalk : hostIdle;
            canvas.drawBitmap(host, 666, 210, paint);
            drawHostText(canvas);

            if (state == WAIT_LETTER && currentPlayer == 0) {
                drawAlphabet(canvas);
            } else if (state == PRIZE_CHOICE && currentPlayer == 0) {
                drawClassicButton(canvas, prizeButton, "Приз!", true);
                drawClassicButton(canvas, playButton, "Играем", true);
            } else {
                drawClassicButton(canvas, spinButton, "Вращать Барабан", currentPlayer == 0 && state == WAIT_SPIN);
                drawClassicButton(canvas, wordButton, "Сказать слово",
                        currentPlayer == 0 && (state == WAIT_SPIN || state == WAIT_LETTER));
            }
        }

        private void drawBoard(Canvas canvas) {
            int length = Math.min(answer.length(), BOARD_SLOTS);
            int start = (BOARD_SLOTS - length) / 2;
            for (int slot = 0; slot < BOARD_SLOTS; slot++) {
                int answerIndex = slot - start;
                float left = 288 + slot * 24;
                RectF tile = new RectF(left, 72, left + 17, 89);
                if (answerIndex < 0 || answerIndex >= answer.length()) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(238, 238, 238));
                    canvas.drawRect(tile, paint);
                } else if (opened[answerIndex]) {
                    paint.setStyle(Paint.Style.FILL);
                    paint.setColor(Color.rgb(225, 225, 225));
                    canvas.drawRect(tile, paint);
                    paint.setColor(Color.BLACK);
                    paint.setTextAlign(Paint.Align.CENTER);
                    paint.setTextSize(15);
                    paint.setFakeBoldText(true);
                    canvas.drawText(String.valueOf(answer.charAt(answerIndex)), tile.centerX(), 86, paint);
                    paint.setFakeBoldText(false);
                }
            }

            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(11);
            paint.setFakeBoldText(false);
            canvas.drawText("Тема игры: " + category, 448, 139, paint);
        }

        private void drawLabels(Canvas canvas) {
            paint.setColor(Color.rgb(70, 70, 70));
            paint.setTextSize(11);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(names[2], 121, 202, paint);
            canvas.drawText("Третий игрок", 121, 226, paint);
            canvas.drawText(names[1], 548, 202, paint);
            canvas.drawText("Второй игрок", 548, 226, paint);
            canvas.drawText("Очки:  " + scores[0], 59, 395, paint);

            paint.setTextSize(10);
            canvas.drawText("Очки: " + scores[2], 120, 239, paint);
            canvas.drawText("Очки: " + scores[1], 548, 239, paint);
        }

        private void drawHostText(Canvas canvas) {
            paint.setColor(Color.BLACK);
            paint.setTextSize(11);
            paint.setFakeBoldText(true);
            paint.setTextAlign(Paint.Align.LEFT);
            String[] lines = hostText.split("\\n");
            float y = 228;
            for (String line : lines) {
                canvas.drawText(line, 679, y, paint);
                y += 15;
            }
            paint.setFakeBoldText(false);
        }

        private void drawClassicButton(Canvas canvas, RectF rect, String text, boolean enabled) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? Color.rgb(212, 208, 200) : Color.rgb(190, 190, 190));
            canvas.drawRect(rect, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(enabled ? Color.WHITE : Color.rgb(225, 225, 225));
            canvas.drawLine(rect.left, rect.top, rect.right, rect.top, paint);
            canvas.drawLine(rect.left, rect.top, rect.left, rect.bottom, paint);
            paint.setColor(Color.rgb(64, 64, 64));
            canvas.drawLine(rect.right, rect.top, rect.right, rect.bottom, paint);
            canvas.drawLine(rect.left, rect.bottom, rect.right, rect.bottom, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(enabled ? Color.BLACK : Color.rgb(110, 110, 110));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(text.length() > 12 ? 9 : 11);
            float baseline = rect.centerY() - (paint.ascent() + paint.descent()) / 2f;
            canvas.drawText(text, rect.centerX(), baseline, paint);
        }

        private void drawAlphabet(Canvas canvas) {
            RectF panel = new RectF(4, 444, 892, 525);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(212, 208, 200));
            canvas.drawRect(panel, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            paint.setColor(Color.WHITE);
            canvas.drawLine(panel.left, panel.top, panel.right, panel.top, paint);
            canvas.drawLine(panel.left, panel.top, panel.left, panel.bottom, paint);
            paint.setColor(Color.rgb(64, 64, 64));
            canvas.drawLine(panel.right, panel.top, panel.right, panel.bottom, paint);
            canvas.drawLine(panel.left, panel.bottom, panel.right, panel.bottom, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.BLACK);
            paint.setTextSize(10);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Назовите букву", 14, 454, paint);

            for (int i = 0; i < ALPHABET.length(); i++) {
                RectF r = alphabetRect(i);
                boolean used = usedLetters.contains(ALPHABET.charAt(i));
                drawClassicButton(canvas, r, String.valueOf(ALPHABET.charAt(i)), !used);
            }
        }

        private RectF alphabetRect(int index) {
            int cols = 16;
            int row = index / cols;
            int col = index % cols;
            float gap = 3;
            float width = 49;
            float left = 13 + col * (width + gap);
            float top = 461 + row * 31;
            return new RectF(left, top, left + width, top + 27);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() != MotionEvent.ACTION_UP) return true;
            float scale = Math.min(getWidth() / WORLD_W, getHeight() / WORLD_H);
            float dx = (getWidth() - WORLD_W * scale) / 2f;
            float dy = (getHeight() - WORLD_H * scale) / 2f;
            float x = (event.getX() - dx) / scale;
            float y = (event.getY() - dy) / scale;
            if (x < 0 || y < 0 || x > WORLD_W || y > WORLD_H) return true;

            if (state == ROUND_OVER && wheelRect.contains(x, y)) {
                newRound();
                return true;
            }
            if (currentPlayer != 0) return true;

            if (state == PRIZE_CHOICE) {
                if (prizeButton.contains(x, y)) choosePrize();
                else if (playButton.contains(x, y)) keepPlaying();
                return true;
            }

            if (state == WAIT_LETTER) {
                for (int i = 0; i < ALPHABET.length(); i++) {
                    if (alphabetRect(i).contains(x, y) && !usedLetters.contains(ALPHABET.charAt(i))) {
                        processLetter(ALPHABET.charAt(i));
                        return true;
                    }
                }
            }

            if (state == WAIT_SPIN && (spinButton.contains(x, y) || wheelRect.contains(x, y))) {
                spinWheel();
            } else if ((state == WAIT_SPIN || state == WAIT_LETTER) && wordButton.contains(x, y)) {
                guessWord();
            }
            return true;
        }
    }

    private static final class WordEntry {
        final String category;
        final String answer;

        WordEntry(String category, String answer) {
            this.category = category;
            this.answer = answer;
        }
    }
}
