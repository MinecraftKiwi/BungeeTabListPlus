/*
 * BungeeTabListPlus - a BungeeCord plugin to customize the tablist
 *
 * Copyright (C) 2014 - 2015 Florian Stober
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package codecrafter47.bungeetablistplus.config;

import codecrafter47.bungeetablistplus.BungeeTabListPlus;
import codecrafter47.bungeetablistplus.api.ITabList;
import codecrafter47.bungeetablistplus.api.ITabListProvider;
import codecrafter47.bungeetablistplus.layout.Layout;
import codecrafter47.bungeetablistplus.layout.LayoutException;
import codecrafter47.bungeetablistplus.layout.TablistLayoutManager;
import codecrafter47.bungeetablistplus.section.AutoFillPlayers;
import codecrafter47.bungeetablistplus.section.Section;
import codecrafter47.bungeetablistplus.tablist.SlotTemplate;
import codecrafter47.bungeetablistplus.tablist.TabListContext;
import com.google.common.base.Joiner;
import com.google.common.collect.HashMultimap;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.*;

/**
 * @author Florian Stober
 */
public class TabListProvider implements ITabListProvider {

    private final BungeeTabListPlus plugin;
    private final List<Section> top;
    private final List<Section> bot;
    private final boolean showEmptyGroups;
    private final TabListConfig config;
    private final ConfigParser parser;
    private final TablistLayoutManager<Section> layoutManager = new TablistLayoutManager<>();
    private final boolean showHeaderFooter;
    private final SlotTemplate header;
    private final SlotTemplate footer;

    public TabListProvider(BungeeTabListPlus plugin, List<Section> top, List<Section> bot,
                           boolean showEmpty, TabListConfig config, ConfigParser parser, boolean showHeaderFooter, SlotTemplate header, SlotTemplate footer) {
        this.top = top;
        this.bot = bot;
        showEmptyGroups = showEmpty;
        this.config = config;
        this.parser = parser;
        this.plugin = plugin;
        this.showHeaderFooter = showHeaderFooter;
        this.header = header;
        this.footer = footer;
    }

    @Override
    public void fillTabList(ProxiedPlayer player, ITabList tabList, TabListContext context) throws LayoutException {
        if (config.verticalMode) {
            tabList = tabList.flip();
        }

        if (config.autoShrinkTabList) {
            tabList.setShouldShrink(true);
        }

        List<Section> topSections = new ArrayList<>(top);
        List<Section> botSections = new ArrayList<>(bot);

        parseAutoFillplayers(player, topSections, context);
        parseAutoFillplayers(player, botSections, context);

        // precalculate all sections
        precalculateSections(context, topSections);
        precalculateSections(context, botSections);

        // remove empty sections
        for (Iterator<Section> iterator = topSections.iterator(); iterator.hasNext(); ) {
            Section topSection = iterator.next();
            if (topSection.getMaxSize() == 0) {
                iterator.remove();
            }
        }
        for (Iterator<Section> iterator = botSections.iterator(); iterator.hasNext(); ) {
            Section botSection = iterator.next();
            if (botSection.getMaxSize() == 0) {
                iterator.remove();
            }
        }


        // calc tablist
        Layout<Section> layout = layoutManager.calculateLayout(topSections, botSections, context);
        for (int i = 0; i < tabList.getSize(); i++) {
            Optional<Layout<Section>.SlotData> slotData = layout.getSlotData(i);
            if (slotData.isPresent()) {
                Layout<Section>.SlotData data = slotData.get();
                tabList.setSlot(i, data.getSection().getSlotAt(context, data.getSlotIndex(), data.getSectionSize()));
            }
        }

        // header + footer
        if (showHeaderFooter) {
            tabList.setHeader(header.buildSlot(context).getText());
            tabList.setFooter(footer.buildSlot(context).getText());
        }

        if (BungeeTabListPlus.isVersion18()) {
            tabList.setDefaultSkin(plugin.getSkinManager().getSkin(config.defaultSkin));
        }

        tabList.setDefaultPing(config.defaultPing);
    }

    private void precalculateSections(TabListContext context, List<Section> topSections) {
        for (Section section : topSections) {
            section.precalculate(context);
        }
    }

    private void parseAutoFillplayers(final ProxiedPlayer player, List<Section> sectionList, TabListContext context) {
        for (int i = 0; i < sectionList.size(); i++) {
            Section section = sectionList.get(i);
            if (section instanceof AutoFillPlayers) {
                sectionList.remove(i);
                SlotTemplate prefix = ((AutoFillPlayers) section).prefix;
                SlotTemplate suffix = ((AutoFillPlayers) section).suffix;
                int maxPlayers = ((AutoFillPlayers) section).maxPlayers;
                List<String> sortRules = ((AutoFillPlayers) section).sortRules;

                Map<String, ServerInfo> servers = ProxyServer.getInstance().
                        getServers();

                Set<String> serverSet = new HashSet<>(servers.keySet());
                HashMultimap<String, String> aliasToServerMap = HashMultimap.create();
                for (Map.Entry<String, String> entry : BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().serverAlias.entrySet()) {
                    if (ProxyServer.getInstance().getServerInfo(entry.getKey()) == null) {
                        BungeeTabListPlus.getInstance().getLogger().warning("Configuration Error: Server \"" + entry.getKey() + "\" used in the alias map does not exist.");
                        continue;
                    }
                    aliasToServerMap.put(entry.getValue(), entry.getKey());
                }
                List<String> list = new ArrayList<>();
                while (!serverSet.isEmpty()) {
                    String server = serverSet.iterator().next();
                    String alias = BungeeTabListPlus.getInstance().getConfigManager().getMainConfig().serverAlias.get(server);
                    if (alias != null) {
                        Set<String> strings = aliasToServerMap.get(alias);
                        serverSet.removeAll(strings);
                        list.add(Joiner.on(',').join(strings));
                    } else {
                        serverSet.remove(server);
                        list.add(server);
                    }
                }

                Collections.sort(list, new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        int p1 = context.
                                getPlayerManager().getServerPlayerCount(s1, player, plugin.getConfigManager().getMainConfig().showPlayersInGamemode3);
                        int p2 = context.
                                getPlayerManager().getServerPlayerCount(s2, player, plugin.getConfigManager().getMainConfig().showPlayersInGamemode3);
                        if (p1 < p2) {
                            return 1;
                        }
                        if (p1 > p2) {
                            return -1;
                        }
                        return s1.compareTo(s2);
                    }
                });

                int j = i;
                for (String server : list) {
                    if (showEmptyGroups || context.getPlayerManager().
                            getPlayerCount(Collections.singletonList(server), player, plugin.getConfigManager().getMainConfig().showPlayersInGamemode3) > 0) {
                        List<Section> sections = parser.
                                parseServerSections(config, prefix, suffix, new ArrayList<>(0), server,
                                        sortRules, maxPlayers, ((AutoFillPlayers) section).playerLines, ((AutoFillPlayers) section).morePlayerLines);
                        for (Section s : sections) {
                            sectionList.add(j++, s);
                        }
                    }
                }
            }
        }
    }

    public boolean appliesTo(ProxiedPlayer player) {
        return config.appliesTo(player);
    }
}
