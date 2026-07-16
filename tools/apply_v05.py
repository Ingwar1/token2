from pathlib import Path

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

needle = "private void advanceAfterRound()"
pos = text.find(needle)
if pos >= 0:
    line_start = text.rfind("\n", 0, pos) + 1
    brace_start = text.find("{", pos)
    depth = 0
    method_end = -1
    for i in range(brace_start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                method_end = i + 1
                break
    if method_end > 0:
        text = text[:line_start] + new_advance + text[method_end:]
        print("advanceAfterRound replaced")
    else:
        print("advanceAfterRound closing brace not found")
else:
    print("advanceAfterRound not found")

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

path.write_text(text, encoding="utf-8")
print("Applied v0.5 single-round restoration")
