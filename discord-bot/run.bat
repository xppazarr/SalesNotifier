@echo off
echo ======================================
echo  Sales Notifier Discord Bot
echo ======================================
echo.
echo Make sure to configure your bot token and channel ID in bot.js first!
echo.
echo Required setup:
echo 1. Create Discord bot at https://discord.com/developers/applications
echo 2. Set DISCORD_TOKEN in bot.js
echo 3. Set WEBHOOK_CHANNEL_ID in bot.js
echo 4. Add  the bot to your server give full permissions
echo 4. Set webhook URL in Minecraft: /salesnotifier webhook http://localhost:3000/webhook
echo.
echo Starting bot...
echo.
npm start
pause
