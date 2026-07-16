from pathlib import Path
import re

path = Path("app/src/main/java/com/ingwar/barabanchudes/MainActivity.java")
text = path.read_text(encoding="utf-8")

# Keep the original single-round flow instead of the invented tournament.
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

text, advance_count = re.subn(
    r'^        private void advanceAfterRound\(\) \{.*?^        \}',
    new_advance,
    text,
    count=1,
    flags=re.MULTILINE | re.DOTALL,
)

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

if advance_count != 1:
    raise SystemExit("advanceAfterRound method was not found")
if 'Победа в финале!' in text or 'Вы прошли все три тура' in text:
    raise SystemExit("Tournament ending is still present")
if 'hostSay(roundTitles[roundNumber - 1]' in text:
    raise SystemExit("Tournament greeting is still active")

path.write_text(text, encoding="utf-8")
print("Applied v0.5 single-round restoration")
