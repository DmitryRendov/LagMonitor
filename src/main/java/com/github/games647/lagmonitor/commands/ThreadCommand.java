package com.github.games647.lagmonitor.commands;

import com.github.games647.lagmonitor.LagMonitor;
import com.github.games647.lagmonitor.Pagination;
import com.google.common.collect.Lists;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import javax.management.InstanceNotFoundException;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

import org.apache.commons.lang.ArrayUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class ThreadCommand extends DumpCommand {

    private static final ChatColor PRIMARY_COLOR = ChatColor.DARK_AQUA;
    private static final ChatColor SECONDARY_COLOR = ChatColor.GRAY;

    //https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/DiagnosticCommandMBean.html
    private static final String DIAGNOSTIC_COMMAND = "com.sun.management:type=DiagnosticCommand";
    private static final String DUMP_COMMAND = "threadPrint";

    public ThreadCommand(LagMonitor plugin) {
        super(plugin, "thread", "tdump");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!isAllowed(sender, command)) {
            sender.sendMessage(org.bukkit.ChatColor.DARK_RED + "Not whitelisted");
            return true;
        }

        if (args.length > 0) {
            String subCommand = args[0];
            if ("dump".equalsIgnoreCase(subCommand)) {
                onDump(sender);
            } else {
                sender.sendMessage(label);
            }

            return true;
        }

        List<BaseComponent[]> lines = Lists.newArrayList();

        Map<Thread, StackTraceElement[]> allStackTraces = Thread.getAllStackTraces();
        for (Thread thread : allStackTraces.keySet()) {
            if (thread.getContextClassLoader() == null) {
                //ignore java system threads like reference handler
                continue;
            }

            BaseComponent[] components = new ComponentBuilder(thread.getName())
                    .color(PRIMARY_COLOR)
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND
                            , "/stacktrace " + thread.getName()))
                    .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT
                            , new ComponentBuilder("Show the stacktrace").create()))
                    .append("-" + thread.getId() + " State: ")
                    .color(ChatColor.GOLD)
                    .append(thread.getState().toString())
                    .color(SECONDARY_COLOR)
                    .create();
            lines.add(components);
        }

        Pagination pagination = new Pagination("Threads", lines);
        pagination.send(sender);
        plugin.getPaginations().put(sender, pagination);
        return true;
    }

    private void onDump(CommandSender sender) {
        try {
            String result = (String) invokeBeanCommand(DIAGNOSTIC_COMMAND, DUMP_COMMAND
                    , new Object[]{ArrayUtils.EMPTY_STRING_ARRAY}, new String[]{String[].class.getName()});

            Path dumpFile = getNewDumpFile();
            Files.write(dumpFile, Lists.newArrayList(result));

            sender.sendMessage(ChatColor.GRAY + "Dump created: " + dumpFile.getFileName());
            sender.sendMessage(ChatColor.GRAY + "You can analyse it using VisualVM");
        } catch (InstanceNotFoundException instanceNotFoundException) {
            plugin.getLogger().log(Level.SEVERE, "You are not using Oracle JVM. OpenJDK hasn't implemented it yet"
                    , instanceNotFoundException);
            sender.sendMessage(ChatColor.DARK_RED + "You are not using Oracle JVM. OpenJDK hasn't implemented it yet");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, null, ex);
            sender.sendMessage(ChatColor.DARK_RED + "An exception occurred. Please check the server log");
        }
    }
}
