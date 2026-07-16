from pathlib import Path
import re

path = Path("app/src/main/java/com/ingwar/barabanchudes/MainActivity.java")
text = path.read_text(encoding="utf-8")

text = text.replace(
    'hostSay(roundTitles[roundNumber - 1] + ".\\nПервый игрок, вращайте барабан!");',
    'hostSay("Первый игрок, вращайте\\nбарабан!");',
)

new_advance = '''        private void advanceAfterRound() {
            if (state != ROUND_OVER) return;
            if (roundWinner == 0) {
                String prize = prizes[random.nextInt(prizes.length)];
                endGame("Вы победили!", "Угадано слово «" + answer + "».\\nВаш приз — " + prize + ".\\nОчки: " + scores[0] + ".");
            } else {
                endGame("Вы проиграли", names[roundWinner] + " угадал слово «" + answer + "».\\nЕго очки: " + scores[roundWinner] + ".");
            }
        }'''

pattern = re.compile(
    r'^        private void advanceAfterRound\(\) \{.*?^        \}',
    flags=re.MULTILINE | re.DOTALL,
)
text, count = pattern.subn(lambda _match: new_advance, text, count=1)
print(f"advanceAfterRound replacements: {count}")

text = text.replace('drawClassicButton(canvas, prizeButton, "Забрать приз", true);',
                    'drawClassicButton(canvas, prizeButton, "Приз!", true);')
text = text.replace('drawClassicButton(canvas, playButton, "Играть дальше", true);',
                    'drawClassicButton(canvas, playButton, "Играем", true);')
text = text.replace(
    'canvas.drawText(roundTitles[roundNumber - 1] + " — тур " + roundNumber + " из 3", 448, 154, paint);',
    'canvas.drawText("Играют три участника", 448, 154, paint);',
)
text = text.replace(
    'hostSay(text + "\\nКоснитесь барабана.");',
    'hostSay(text + "\\nКоснитесь барабана, чтобы получить приз.");',
)

if count != 1:
    raise SystemExit("advanceAfterRound method was not found")

path.write_text(text, encoding="utf-8")
print("Applied v0.5 single-round restoration")
