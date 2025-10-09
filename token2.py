import os
import random
import time
import requests
from telegram import Bot, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import Updater, CommandHandler

# Загружаем переменные из окружения
TOKEN = os.getenv("TOKEN")  # Токен бота Telegram
CHAT_ID = os.getenv("CHAT_ID")  # ID чата или канала куда отправлять сообщения
REF_LINKS = [
    {"label": "Bybit", "url": "https://partner.bybit.com/b/ВАШ_РЕФ_КОД"},
    {"label": "Bitget", "url": "https://partner.bitget.com/b/ВАШ_РЕФ_КОД"}
]

bot = Bot(token=TOKEN)

def start(update, context):
    update.message.reply_text("👋 Привет! Я показываю новые токены с DEX Screener.")

def choose_ref_link(user_id):
    # Привязка A/B теста к user_id чтобы сохранялась ссылка для пользователя
    random.seed(user_id)
    return random.choice(REF_LINKS)

def check_new_tokens():
    url = "https://api.dexscreener.com/latest/dex/tokens"
    try:
        data = requests.get(url).json()
        for token in data["pairs"][:5]:  # последние 5 токенов
            name = token["baseToken"]["name"]
            symbol = token["baseToken"]["symbol"]
            price = float(token["priceUsd"])
            liquidity = token.get("liquidity", "N/A")
            vol = token["volume"]["h24"]

            text = f"🪙 Новый токен: {name} ({symbol})
💰 Цена: ${price:.8f}
📊 Объем 24ч: ${vol}
💧 Ликвидность: {liquidity}"
            # Для демонстрации выбираем ссылку рандомно (можно заменить user_id на нужный)
            ref_link = choose_ref_link(random.randint(1, 100000))["url"]
            btn = [[InlineKeyboardButton("💸 Купить", url=ref_link)]]

            bot.send_message(chat_id=CHAT_ID, text=text, reply_markup=InlineKeyboardMarkup(btn))
            time.sleep(1)  # чтобы не спамить быстро
    except Exception as e:
        print("Ошибка при получении данных или отправке сообщения:", e)

def main():
    updater = Updater(TOKEN)
    dp = updater.dispatcher
    dp.add_handler(CommandHandler("start", start))
    updater.start_polling()

    while True:
        check_new_tokens()
        time.sleep(300)  # каждые 5 минут

if __name__ == "__main__":
    main()
