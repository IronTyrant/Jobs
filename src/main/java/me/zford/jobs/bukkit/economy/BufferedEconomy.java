package me.zford.jobs.bukkit.economy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.milkbowl.vault.economy.Economy;

import me.zford.jobs.bukkit.JobsPlugin;
import me.zford.jobs.bukkit.tasks.BufferedPaymentTask;
import me.zford.jobs.container.JobsPlayer;
import me.zford.jobs.economy.BufferedPayment;

public class BufferedEconomy {
    
    private Map<String, Double> payments = Collections.synchronizedMap(new LinkedHashMap<String, Double>());
    private JobsPlugin plugin;
    public BufferedEconomy(JobsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Add payment to player's payment buffer
     * @param player - player to be paid
     * @param amount - amount to be paid
     */
    public void pay(JobsPlayer player, double amount) {
        if (amount == 0)
            return;
        double total = 0;
        String playername = player.getName();
        
        synchronized (payments) {
            if (payments.containsKey(playername))
                total = payments.get(playername);
            
            total += amount;
            payments.put(player.getName(), total);
        }
    }
    
    /**
     * Payout all players the amount they are going to be paid
     */
    public void payAll(Economy economy) {
        if (payments.isEmpty())
            return;
        
        int batchSize = plugin.getJobsConfiguration().getEconomyBatchSize();
        ArrayList<BufferedPayment> buffered = new ArrayList<BufferedPayment>(batchSize);
        synchronized (payments) {
            for (Map.Entry<String, Double> entry : payments.entrySet()) {
                if (buffered.size() >= batchSize) {
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BufferedPaymentTask(economy, buffered));
                    buffered = new ArrayList<BufferedPayment>(batchSize);
                }
                String playername = entry.getKey();
                double payment = entry.getValue().doubleValue();
                if (payment != 0)
                    buffered.add(new BufferedPayment(playername, payment));
            }
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new BufferedPaymentTask(economy, buffered));
            
            payments.clear();
        }
    }
}
