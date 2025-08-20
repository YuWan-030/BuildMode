
// 伪创造GUI 1.20.1 增强版
package me.alini.buildmode.client;

import me.alini.buildmode.network.C2SRequestItemPacket;
import me.alini.buildmode.network.C2SRequestToggleBuildModePacket;
import me.alini.buildmode.network.C2SWhitelistEditPacket;
import me.alini.buildmode.network.ModMessages;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.CompletableFuture;

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

    private final Map<ResourceLocation, Status> tempEdits = new HashMap<>();
    private Set<ResourceLocation> originalWhitelist = new HashSet<>();

    private boolean loading = false;

    private enum Status {
        WHITELISTED,
        TO_ADD,
        TO_REMOVE
    }

    public BuildModeScreen() {
        super(Component.translatable("buildmode.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        // 在这里添加按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("buildmode.button.tooltip"),
                button -> {
                    ModMessages.getChannel().sendToServer(new C2SRequestToggleBuildModePacket());
                    Minecraft.getInstance().setScreen(null);
                }
        ).bounds(this.width / 2 - 50, this.height / 2, 100, 20).build());
        originalWhitelist = new HashSet<>(ClientWhitelist.getWhitelist());
        if (tabList == null) {
            tabList = new ArrayList<>(BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());
        }
        if (allItems == null) {
            allItems = new ArrayList<>();
            showLoadingIndicator();
            loading = true;
            CompletableFuture.runAsync(() -> {
                Set<Item> items = new HashSet<>();
                for (CreativeModeTab tab : tabList) {
                    for (ItemStack stack : tab.getDisplayItems()) {
                        items.add(stack.getItem());
                    }
                }
                List<Item> itemList = new ArrayList<>(items);
                itemList.sort(Comparator.comparing(i -> ForgeRegistries.ITEMS.getKey(i).toString()));
                Minecraft.getInstance().execute(() -> {
                    allItems.clear();
                    allItems.addAll(itemList);
                    loading = false;
                    hideLoadingIndicator();
                    updateFilteredItems();
                });
            });
        }
        int tabCount = tabList.size();
        int buttonWidth = 48;
        int buttonHeight = 18;
        int padding = 8;
        int availableWidth = this.width - 2 * padding;
        int tabsPerRow = Math.max(1, availableWidth / (buttonWidth + padding));
        int rows = (int) Math.ceil((double) tabCount / tabsPerRow);

        int startY = 35;
        for (int row = 0; row < rows; row++) {
            int startX = (this.width - Math.min(tabsPerRow, tabCount - row * tabsPerRow) * (buttonWidth + padding)) / 2;
            for (int col = 0; col < tabsPerRow; col++) {
                int index = row * tabsPerRow + col;
                if (index >= tabCount) break;
                CreativeModeTab tab = tabList.get(index);
                int x = startX + col * (buttonWidth + padding);
                int y = startY + row * (buttonHeight + padding);
                this.addRenderableWidget(Button.builder(Component.translatable(tab.getDisplayName().getString()), b -> {
                    currentTab = tab;
                    currentPage = 0;
                    updateFilteredItems();
                }).bounds(x, y, buttonWidth, buttonHeight).build());
            }
        }

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

        // 搜索框初始化
        if (searchBox == null) {
            searchBox = new EditBox(this.font, this.width / 2 - 80, 10, 160, 18, Component.literal("Search"));
            searchBox.setResponder(this::onSearchChanged);
            this.addRenderableWidget(searchBox);
        }

        updateFilteredItems();
    }

    private void showLoadingIndicator() {
        // 可自定义加载中提示
    }

    private void hideLoadingIndicator() {
        // 可自定义隐藏加载中提示
    }

    private void onSearchChanged(String query) {
        currentPage = 0;
        updateFilteredItems();
    }

    public void updateFilteredItems() {
        if (loading || allItems == null || allItems.isEmpty()) {
            filteredItems = Collections.emptyList();
            totalPages = 1;
            currentPage = 0;
            return;
        }
        if (currentTab == null && !tabList.isEmpty()) {
            currentTab = tabList.get(0);
        }
        String query = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
        List<Item> tabItems = currentTab != null
                ? currentTab.getDisplayItems().stream().map(ItemStack::getItem).distinct().toList()
                : allItems;

        if (filteredItems != null && query.equals(lastQuery) && currentTab == lastTab) {
            return;
        }
        lastQuery = query;
        lastTab = currentTab;

        filteredItems = new ArrayList<>();
        for (Item item : tabItems) {
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
        if (searchBox != null) searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);

        if (loading) {
            String loadingText = "加载中...";
            guiGraphics.drawString(this.font, loadingText, this.width / 2 - this.font.width(loadingText) / 2, this.height / 2, 0xFFFFFF);
            super.render(guiGraphics, mouseX, mouseY, partialTicks);
            return;
        }

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

                Status status = tempEdits.get(id);
                int color;
                if (status == null) {
                    color = 0x80000000;
                } else {
                    color = switch (status) {
                        case WHITELISTED -> 0x8000FF00;
                        case TO_ADD -> 0x800000FF;
                        case TO_REMOVE -> 0x80FF0000;
                    };
                }
                guiGraphics.fill(x, y, x + slotSize, y + slotSize, color);

                ItemStack stack = new ItemStack(item);
                guiGraphics.renderItem(stack, x + 2, y + 2);

                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
                index++;
            }
        }

        String pageInfo = (currentPage + 1) + "/" + totalPages;
        guiGraphics.drawString(this.font, pageInfo, this.width / 2 - this.font.width(pageInfo) / 2, this.height - 45, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && searchBox.isFocused() && searchBox.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (loading || filteredItems == null) return false;

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

                    if (button == 0) {
                        ModMessages.getChannel().sendToServer(new C2SRequestItemPacket(id));
                    } else if (button == 2) {
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