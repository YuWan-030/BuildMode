// 伪创造GUI 1.20.1 增强版
package me.alini.buildmode.client;

import me.alini.buildmode.network.C2SRequestItemPacket;
import me.alini.buildmode.network.C2SWhitelistEditPacket;
import me.alini.buildmode.network.ModMessages;
import me.alini.buildmode.network.S2CSyncWhitelistPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class BuildModeScreen extends Screen {
    private final int columns = 9;
    private final int rows = 5;
    private final int slotSize = 20;
    private final int itemsPerPage = columns * rows;
    private String lastQuery = "";
    private CreativeModeTab lastTab = null;

    private EditBox searchBox;
    private int currentPage = 0;
    private int totalPages = 1;
    private CreativeModeTab currentTab = null;
    private List<CreativeModeTab> tabList;
    private List<Item> allItems;
    private List<Item> filteredItems;

    // 临时修改状态：添加/删除的物品
    private final Map<ResourceLocation, Status> tempEdits = new HashMap<>();
    private Set<ResourceLocation> originalWhitelist = new HashSet<>();

    private enum Status {
        WHITELISTED,   // 绿色
        TO_ADD,        // 蓝色
        TO_REMOVE      // 红色
    }

    public BuildModeScreen() {
        super(Component.translatable("buildmode.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        // 获取原始白名单
        Set<ResourceLocation> whitelist = ClientWhitelist.getWhitelist();
        originalWhitelist = new HashSet<>(whitelist);

        // 获取所有物品和标签页
        allItems = new ArrayList<>(ForgeRegistries.ITEMS.getValues());
        tabList = BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList();
        currentTab = tabList.get(0);

        // 初始化白名单状态
        tempEdits.clear();
        for (Item item : allItems) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (whitelist.contains(id)) {
                tempEdits.put(id, Status.WHITELISTED);
            }
        }

        // 搜索框
        searchBox = new EditBox(this.font, this.width / 2 - 100, 10, 200, 20, Component.translatable("buildmode.screen.search"));
        searchBox.setResponder(this::onSearchChanged);
        this.addRenderableWidget(searchBox);

        // 标签页按钮
        int tabX = this.width / 2 - (tabList.size() * 50) / 2;
        for (int i = 0; i < tabList.size(); i++) {
            CreativeModeTab tab = tabList.get(i);
            int x = tabX + i * 50;
            this.addRenderableWidget(Button.builder(Component.translatable(tab.getDisplayName().getString()), b -> {
                currentTab = tab;
                currentPage = 0;
                updateFilteredItems();
            }).bounds(x, 35, 48, 18).build());
        }

        // 翻页按钮
        int btnY = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            if (currentPage > 0) {
                currentPage--;
            }
        }).bounds(this.width / 2 - 60, btnY, 30, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
            }
        }).bounds(this.width / 2 + 30, btnY, 30, 20).build());

        updateFilteredItems();
    }

    private void onSearchChanged(String query) {
        currentPage = 0;
        updateFilteredItems();
    }

    // 过滤物品
    public void updateFilteredItems() {
        String query = searchBox.getValue().toLowerCase(Locale.ROOT);
        List<Item> tabItems = currentTab.getDisplayItems().stream()
                .map(ItemStack::getItem)
                .distinct()
                .toList();

        // 缓存过滤结果
        if (filteredItems != null && query.equals(lastQuery) && currentTab == lastTab) {
            return;
        }
        lastQuery = query;
        lastTab = currentTab;

        // 提前终止过滤
        filteredItems = new ArrayList<>();
        for (Item item : tabItems) {
            if (filteredItems.size() >= itemsPerPage * totalPages) break;
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id.toString().toLowerCase(Locale.ROOT).contains(query)
                    || item.getDescription().getString().toLowerCase(Locale.ROOT).contains(query)) {
                filteredItems.add(item);
            }
        }

        totalPages = Math.max(1, (filteredItems.size() + itemsPerPage - 1) / itemsPerPage);
        if (currentPage >= totalPages) currentPage = totalPages - 1;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics);
        searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 渲染物品网格
        int startX = (this.width - columns * slotSize) / 2;
        int startY = 60;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();

        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(filteredItems.size(), startIdx + itemsPerPage);

        int index = startIdx;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (index >= endIdx) break;
                Item item = filteredItems.get(index);
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);

                int x = startX + col * slotSize;
                int y = startY + row * slotSize;

                // 渲染背景颜色
                Status status = tempEdits.get(id);
                int color = switch (status) {
                    case WHITELISTED -> 0x8000FF00;
                    case TO_ADD -> 0x800000FF;
                    case TO_REMOVE -> 0x80FF0000;
                    default -> 0x80000000;
                };
                guiGraphics.fill(x, y, x + slotSize, y + slotSize, color);

                // 渲染物品
                ItemStack stack = new ItemStack(item);
                guiGraphics.renderItem(stack, x + 2, y + 2);

                // 悬停提示
                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
                index++;
            }
        }

        // 渲染页码
        String pageInfo = (currentPage + 1) + "/" + totalPages;
        guiGraphics.drawString(this.font, pageInfo, this.width / 2 - this.font.width(pageInfo) / 2, this.height - 45, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox.isFocused() && searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        int startX = (this.width - columns * slotSize) / 2;
        int startY = 60;
        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(filteredItems.size(), startIdx + itemsPerPage);

        int index = startIdx;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                if (index >= endIdx) break;
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;

                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    Item item = filteredItems.get(index);
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);

                    if (button == 0) { // 左键请求获取物品
                        ModMessages.getChannel().sendToServer(new C2SRequestItemPacket(id));
                    } else if (button == 2) { // 中键修改白名单
                        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
                            Status current = tempEdits.get(id);
                            if (current == null) {
                                tempEdits.put(id, Status.TO_ADD);
                            } else if (current == Status.WHITELISTED) {
                                tempEdits.put(id, Status.TO_REMOVE);
                            } else {
                                tempEdits.remove(id);
                            }
                        }
                    }
                    return true;
                }
                index++;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        super.onClose();
        // 只同步有变动的白名单
        for (Map.Entry<ResourceLocation, Status> entry : tempEdits.entrySet()) {
            ResourceLocation id = entry.getKey();
            Status status = entry.getValue();
            boolean originallyWhitelisted = originalWhitelist.contains(id);
            if (status == Status.TO_ADD && !originallyWhitelisted) {
                ModMessages.getChannel().sendToServer(new C2SWhitelistEditPacket(id, true));
            } else if (status == Status.TO_REMOVE && originallyWhitelisted) {
                ModMessages.getChannel().sendToServer(new C2SWhitelistEditPacket(id, false));
            }
        }
    }

}