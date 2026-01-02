const { Client, GatewayIntentBits, ActionRowBuilder, StringSelectMenuBuilder, EmbedBuilder } = require('discord.js');
const express = require('express');

// Configuration - UPDATE THESE VALUES
const DISCORD_TOKEN = 'BOT TOKEN'; // Get from https://discord.com/developers/applications
const WEBHOOK_CHANNEL_ID = 'CHANNELID'; // Right-click channel on Discord > Copy ID
const MINECRAFT_IP = 'localhost'; // Don't touch if you are running both the mod and bot in the same computer
const MINECRAFT_PORT = 8080; // Don't touch if you are running both the mod and bot in the same computer

const client = new Client({
    intents: [GatewayIntentBits.Guilds, GatewayIntentBits.GuildMessages]
});

const app = express();
app.use(express.json());

const activeDropdowns = new Map();

app.post('/webhook', (req, res) => {
    console.log('Received webhook:', JSON.stringify(req.body, null, 2));

    try {
        const embeds = req.body.embeds;
        if (embeds && embeds.length > 0) {
            const embed = embeds[0];
            const description = embed.description || '';


            const npcDataMatch = description.match(/\*\*NPC_BUY_DATA:\*\*\s*`([^`]+)`/);
            if (npcDataMatch) {
                const npcList = npcDataMatch[1].split(',');
                console.log('Found NPC data:', npcList);
                createInteractiveMessage(embed, npcList);
            } else {
                console.log('No NPC data found in webhook, sending regular message');
                console.log('Embed title:', embed.title);
                console.log('Embed description length:', embed.description ? embed.description.length : 0);
                sendRegularMessage(embed);
            }
        } else {
            console.log('No embeds found in webhook');
        }
    } catch (error) {
        console.error('Error processing webhook:', error);
    }

    res.sendStatus(200);
});


async function sendRegularMessage(embed) {
    try {
        console.log('Attempting to send regular message...');

        const channel = await client.channels.fetch(WEBHOOK_CHANNEL_ID);
        if (!channel) {
            console.error('Could not find channel:', WEBHOOK_CHANNEL_ID);
            return;
        }

        console.log('Channel found:', channel.name, 'Type:', channel.type);


        const messageEmbed = new EmbedBuilder()
            .setTitle(embed.title || 'Notification')
            .setDescription(embed.description || '')
            .setColor(embed.color || 3066993);

        if (embed.timestamp) {
            messageEmbed.setTimestamp(new Date(embed.timestamp));
        }

        console.log('Created embed with title:', embed.title);

        const messagePayload = {
            embeds: [messageEmbed]
        };

        console.log('Sending message payload:', JSON.stringify(messagePayload, null, 2));

        const sentMessage = await channel.send(messagePayload);

        console.log('Regular message sent successfully with ID:', sentMessage.id);

    } catch (error) {
        console.error('Error sending regular message:', error);
        console.error('Error details:', error.message);
        console.error('Stack trace:', error.stack);
    }
}


async function createInteractiveMessage(originalEmbed, npcList) {
    try {
        const channel = await client.channels.fetch(WEBHOOK_CHANNEL_ID);
        if (!channel) {
            console.error('Could not find channel:', WEBHOOK_CHANNEL_ID);
            return;
        }

        const options = npcList.map((npc, index) => ({
            label: `${index + 1}. ${npc.replace(/_/g, ' ')}`,
            value: npc,
            description: `Buy ${npc.replace(/_/g, ' ')}`
        }));
        console.log('Created options:', options);

        const selectMenu = new StringSelectMenuBuilder()
            .setCustomId('npc_buy')
            .setPlaceholder('Select an NPC to buy')
            .addOptions(options.slice(0, 25)); // Discord limit of 25 options

        const row = new ActionRowBuilder().addComponents(selectMenu);

        const cleanEmbed = new EmbedBuilder()
            .setTitle(originalEmbed.title || 'Merchant Update')
            .setDescription(originalEmbed.description.replace(/\n\*\*NPC_BUY_DATA:\*\*\s*`[^`]+`/, ''))
            .setColor(originalEmbed.color || 3066993);

        if (originalEmbed.timestamp) {
            cleanEmbed.setTimestamp(new Date(originalEmbed.timestamp));
        }

        console.log('Sending interactive message to channel:', WEBHOOK_CHANNEL_ID);

        const message = await channel.send({
            embeds: [cleanEmbed],
            components: [row]
        });

        console.log('Interactive message sent successfully');

        activeDropdowns.set(message.id, Date.now());

    } catch (error) {
        console.error('Error creating interactive message:', error);
    }
}

client.on('interactionCreate', async (interaction) => {
    if (!interaction.isStringSelectMenu()) return;

    if (interaction.customId === 'npc_buy') {
        const selectedNpc = interaction.values[0];

        console.log('User selected NPC:', selectedNpc);

        await interaction.deferReply({ ephemeral: true });

        try {
            const response = await fetch(`http://${MINECRAFT_IP}:${MINECRAFT_PORT}/buy`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    npc: selectedNpc,
                    user: interaction.user.username,
                    timestamp: new Date().toISOString()
                })
            });

            if (response.ok) {
                const result = await response.text();
                console.log('Purchase request sent successfully:', result);
                await interaction.editReply(`✅ Purchase request sent for **${selectedNpc.replace(/_/g, ' ')}**!`);
            } else {
                const errorText = await response.text();
                console.error('Purchase request failed:', response.status, errorText);
                await interaction.editReply('❌ Failed to send purchase request to Minecraft.');
            }
        } catch (error) {
            console.error('Error sending purchase request:', error);
            await interaction.editReply('❌ Error communicating with Minecraft server.');
        }
    }
});


setInterval(() => {
    const now = Date.now();
    for (const [messageId, timestamp] of activeDropdowns.entries()) {
        // Remove messages older than 1 hour
        if (now - timestamp > 3600000) {
            activeDropdowns.delete(messageId);
        }
    }
}, 60000); // Check every minute


client.once('ready', async () => {
    console.log(`Discord bot logged in as ${client.user.tag}`);
    console.log(`Listening for webhooks on port 3000`);
    console.log(`sola.r`);

    try {
        const channel = await client.channels.fetch(WEBHOOK_CHANNEL_ID);
        if (channel) {
            console.log(`✅ Successfully connected to channel: #${channel.name}`);
            console.log(`📝 Channel type: ${channel.type}`);

            const testEmbed = new EmbedBuilder()
                .setTitle('🤖 Bot Started')
                .setDescription('Sales Notifier bot is now online and ready to receive webhooks!')
                .setColor(3066993)
                .setTimestamp();

            await channel.send({ embeds: [testEmbed] });
            console.log('✅ Test message sent successfully');
        } else {
            console.error('❌ Could not find the specified channel. Check WEBHOOK_CHANNEL_ID');
        }
    } catch (error) {
        console.error('❌ Error testing channel access:', error.message);
    }
});


const WEBHOOK_PORT = 3000;
app.listen(WEBHOOK_PORT, () => {
    console.log(` Webhook server listening on port ${WEBHOOK_PORT}`);
    console.log(`📨 Forward Minecraft webhooks to: http://localhost:${WEBHOOK_PORT}/webhook`);
});

client.login(DISCORD_TOKEN).catch(error => {
    console.error('Failed to login to Discord:', error);
    process.exit(1);
});
