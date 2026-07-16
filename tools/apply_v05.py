from pathlib import Path

path = Path("app/src/main/java/com/ingwar/barabanchudes/MainActivity.java")
text = path.read_text(encoding="utf-8")

replacements = {
    '''            hostSay(roundTitles[roundNumber - 1] + ".\\nПервый игрок, вращайте барабан!");''':
    '''            hostSay("Первый игрок, вращайте\\nбарабан!");''',

    '''        private void advanceAfterRound() {
            if (state != ROUND_OVER) return;
            if (roundWinner == 0 && roundNumber < 3) {
                roundNumber++;
                newRound();
            } else if (roundWinner == 0) {
                String prize = prizes[random.nextInt(prizes.length)];
                endGame("Победа в финале!", "Вы прошли все три тура.\\nВаш приз — " + prize + ".");
            } else {
                endGame("Вы проиграли", names[roundWinner] + " угадал слово «" + answer + "».");
            }
        }''':
    '''        private void advanceAfterRound() {
            if (state != ROUND_OVER) return;
            if (roundWinner == 0) {
                String prize = prizes[random.nextInt(prizes.length)];
                endGame("Вы победили!", "Угадано слово «" + answer + "».\\nВаш приз — " + prize + ".\\nОчки: " + scores[0] + ".");
            } else {
                endGame("Вы проиграли", names[roundWinner] + " угадал слово «" + answer + "».\\nЕго очки: " + scores[roundWinner] + ".");
            }
        }''',

    '''                drawClassicButton(canvas, prizeButton, "Забрать приз", true);
                drawClassicButton(canvas, playButton, "Играть дальше", true);''':
    '''                drawClassicButton(canvas, prizeButton, "Приз!", true);
                drawClassicButton(canvas, playButton, "Играем", true);''',

    '''            paint.setTextSize(10);
            canvas.drawText(roundTitles[roundNumber - 1] + " — тур " + roundNumber + " из 3", 448, 154, paint);''':
    '''            paint.setTextSize(10);
            canvas.drawText("Играют три участника", 448, 154, paint);''',

    '''            hostSay(text + "\\nКоснитесь барабана.");''':
    '''            hostSay(text + "\\nКоснитесь барабана, чтобы получить приз.");''',
}

for old, new in replacements.items():
    if old not in text:
        raise SystemExit(f"Expected source fragment not found:\n{old[:160]}")
    text = text.replace(old, new, 1)

# The Windows remake is a single three-player round. Remove the invented
# tournament labels from active logic while leaving harmless fields intact.
text = text.replace('versionName = "0.4"', 'versionName = "0.5"')

path.write_text(text, encoding="utf-8")
print("Applied v0.5 single-round restoration")
