package com.ycjhub.crazyauctionsaddon;

import com.badbones69.crazyauctions.api.events.AuctionBuyEvent;
import com.badbones69.crazyauctions.api.events.AuctionListEvent;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class CrazyAuctionsAddon extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getLogger().info("Plugin by ycj! ");
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        if (getConfig().getBoolean("debug")) {
            test();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @EventHandler
    public void onList(AuctionListEvent e) {
        String itemName = e.getItem().getItemMeta().hasDisplayName()
                ? e.getItem().getItemMeta().getDisplayName()
                : e.getItem().getType().name().replace("_", " ");

        if (getConfig().getBoolean("debug")) {
            Bukkit.getLogger().info("AuctionListEvent 觸發！" + e.getItem() + ":" + e.getPrice() + ":" + e.getPlayer().getName());
        }

        sendAuctionResult(e.getItem(), e.getPlayer().getName(), e.getPrice(), "上架");
        Bukkit.broadcastMessage(ChatColor.of("#a3d977") + ChatColor.translateAlternateColorCodes('&' ,"&l拍賣行系統&r &8» &r玩家: " + e.getPlayer().getName() + " 上架了 " + itemName + " &r售價: " + e.getPrice() ));
    }

    //買入監聽
    @EventHandler
    public void onBuy(AuctionBuyEvent e) {
        String itemName = e.getItem().getItemMeta().hasDisplayName()
                ? e.getItem().getItemMeta().getDisplayName()
                : e.getItem().getType().name().replace("_", " ");

        if (getConfig().getBoolean("debug")) {
            Bukkit.getLogger().info("AuctionBuyEvent 觸發！" + e.getItem() + ":" + e.getPrice() + ":" + e.getPlayer().getName());
        }
        sendAuctionResult(e.getItem(), e.getPlayer().getName(), e.getPrice(), "成交 <:soldout1:1340224283456307282><:soldout2:1340224423474495519>"); //決定後半段文子
        Bukkit.broadcastMessage(ChatColor.of("#a3d977") + ChatColor.translateAlternateColorCodes('&' ,"&l拍賣行系統&r &8» &r玩家: " + e.getPlayer().getName() + " 買下了 " + itemName + " &r售價: " + e.getPrice() ));
    }
    public void sendAuctionResult(ItemStack item, String seller, double price, String action) {
        if (getConfig().getBoolean("debug")) {
            Bukkit.getLogger().info("📢 發送 Webhook: 物品：" + item.getType() + ", 價格：" + price + ", 賣家：" + seller);
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                URL url = new URL(getConfig().getString("webhook"));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                String itemName = item.getItemMeta().hasDisplayName()
                        ? stripColorCodes(item.getItemMeta().getDisplayName())
                        : stripColorCodes(item.getType().name().replace("_", " "));

                String itemLore = item.getItemMeta().hasLore()
                        ? stripColorCodes(String.join("\n", item.getItemMeta().getLore()))
                        : "無額外描述";

                // 轉換換行符號，確保 Discord Webhook 能解析
                itemLore = itemLore.replace("\n", "\\n");


                String imageUrl = "https://minecraft-api.vercel.app/images/items/" + item.getType().name().toLowerCase() + ".png";
                String jsonPayload = """
                {
                                "username": "拍賣行系統",
                                "embeds": [{
                                    "title": "📢 物品%s",
                                    "description": "💰 **價格:** $%.2f 👤 **玩家:** %s",
                                    "color": 15262344,
                                    "fields": [
                                        {
                                            "name": "%s",
                                            "value": "%s",
                                            "inline": true
                                        }
                                    ],
                                    "image": { "url": "%s" },
                                    "footer": {
                                        "text": "使用 /ah 交易物品"
                                    }
                                }]
                            }""".formatted(action, price, seller, itemName, itemLore, imageUrl);

                if (getConfig().getBoolean("debug")) {
                    Bukkit.getLogger().warning(jsonPayload);
                }


                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.getResponseCode(); // 確保請求被發送
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 移除 Minecraft 顏色代碼 (支持 § 和 & 符號)
    // 移除所有 Minecraft 內的顏色代碼和格式碼
    private String stripColorCodes(String text) {
        return text.replaceAll("§[0-9A-Fa-fK-Ok-oRrXxZz]", "").replaceAll("&[0-9A-Fa-fK-Ok-oRrXxZz]", "");
    }


    public void test() {
        try {
            URL url = new URL(getConfig().getString("console"));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonPayload = "{ \"content\": \"測試 Webhook 成功！\" }";

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            Bukkit.getLogger().info("✅ Webhook 發送成功，回應碼：" + responseCode);
        } catch (Exception e) {
            Bukkit.getLogger().severe("❌ Webhook 發送失敗！");
            e.printStackTrace();
        }
    }

}
