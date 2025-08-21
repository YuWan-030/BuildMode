package me.alini.buildmode.client;

import me.alini.buildmode.network.C2SRequestItemPacket;
import me.alini.buildmode.network.C2SWhitelistEditPacket;
import me.alini.buildmode.network.ModMessages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class BuildModeScreen extends Screen {
    private static final int BG_WIDTH = 180;
    private static final int BG_HEIGHT = 136;
    private static final int COLUMNS = 9;
    private static final int ROWS = 5;
    private static final int SLOT_SIZE = 18;
    private static final int TAB_BTN_WIDTH = 24;
    private static final int TAB_BTN_HEIGHT = 28;
    private static final int TAB_BTN_GAP = 4;
    private static final int TABS_Y = -24;
    private static final int ITEMS_X = 9;
    private static final int ITEMS_Y = 18;
    private static final int SEARCH_X = 9;
    private static final int SEARCH_Y = 6;
    private static final int PAGE_BTN_Y = 125;
    private static final int PAGE_BTN_WIDTH = 12;
    private static final int PAGE_BTN_HEIGHT = 17;
    private static final int TABS_PER_PAGE = 6;

    private EditBox searchBox;
    private int currentPage = 0;
    private int totalPages = 1;
    private CreativeModeTab currentTab = null;
    private List<CreativeModeTab> tabList;
    private List<CreativeModeTab> nonEmptyTabs;
    private List<Item> allItems;
    private List<Item> filteredItems;
    private boolean isDragging = false;
    private final Set<Integer> draggingSlots = new HashSet<>();

    private final Map<ResourceLocation, Status> tempEdits = new HashMap<>();
    private Set<ResourceLocation> originalWhitelist = new HashSet<>();
    private boolean loading = false;
    private String lastQuery = "";
    private CreativeModeTab lastTab = null;

    private int bgX, bgY;
    private int tabPage = 0;

    // 动画缓存：格子和标签的 hover 透明度
    private final Map<Integer, Float> slotHoverAlpha = new HashMap<>();
    private final Map<Integer, Float> tabHoverAlpha = new HashMap<>();

    private enum Status {WHITELISTED, TO_ADD, TO_REMOVE}

    public BuildModeScreen() {
        super(Component.translatable("buildmode.screen.title"));
    }

    private List<CreativeModeTab> getNonEmptyTabs() {
        if (tabList == null) return Collections.emptyList();
        List<CreativeModeTab> result = new ArrayList<>();
        for (CreativeModeTab tab : tabList) {
            if (!tab.getDisplayItems().isEmpty()) {
                result.add(tab);
            }
        }
        return result;
    }

    @Override
    protected void init() {
        super.init();
        bgX = (this.width - BG_WIDTH) / 2;
        bgY = (this.height - BG_HEIGHT) / 2;
        originalWhitelist = new HashSet<>(ClientWhitelist.getWhitelist());

        if (tabList == null) {
            tabList = new ArrayList<>(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB.stream().toList());
        }
        nonEmptyTabs = getNonEmptyTabs();

        if (allItems == null) {
            allItems = new ArrayList<>();
            loading = true;
            CompletableFuture.runAsync(() -> {
                Set<Item> items = new HashSet<>();
                for (CreativeModeTab tab : nonEmptyTabs) {
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
                    updateFilteredItems();
                    for (Item item : filteredItems) {
                        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                        if (ClientWhitelist.getWhitelist().contains(id) && !tempEdits.containsKey(id)) {
                            tempEdits.put(id, Status.WHITELISTED);
                        }
                    }
                });
            });
        }

        // 标签页分页
        int totalTabPages = (nonEmptyTabs.size() + TABS_PER_PAGE - 1) / TABS_PER_PAGE;
        int startTab = tabPage * TABS_PER_PAGE;
        int endTab = Math.min(nonEmptyTabs.size(), startTab + TABS_PER_PAGE);
        for (int i = startTab; i < endTab; i++) {
            CreativeModeTab tab = nonEmptyTabs.get(i);
            int x = bgX + 8 + (i - startTab) * (TAB_BTN_WIDTH + TAB_BTN_GAP);
            int y = bgY + TABS_Y - 6;
            this.addRenderableWidget(new TabButton(x, y, TAB_BTN_WIDTH, TAB_BTN_HEIGHT, tab, i));
        }

        // 标签翻页按钮（用 GrayButton 替换，大小与分页按钮一致）
        if (totalTabPages > 1) {
            if (tabPage > 0) {
                this.addRenderableWidget(new GrayButton(
                        bgX - PAGE_BTN_WIDTH - 2 + 5, bgY + TABS_Y + 1, PAGE_BTN_WIDTH, PAGE_BTN_HEIGHT,
                        Component.literal("<"), b -> {
                    tabPage--;
                    this.clearWidgets();
                    this.init();
                }
                ));
            }
            if (tabPage < totalTabPages - 1) {
                this.addRenderableWidget(new GrayButton(
                        bgX + TABS_PER_PAGE * (TAB_BTN_WIDTH + TAB_BTN_GAP) + 8, bgY + TABS_Y + 1, PAGE_BTN_WIDTH, PAGE_BTN_HEIGHT,
                        Component.literal(">"), b -> {
                    tabPage++;
                    this.clearWidgets();
                    this.init();
                }
                ));
            }
        }

        // 搜索框
        if (searchBox == null) {
            searchBox = new EditBox(this.font, bgX + SEARCH_X, bgY + SEARCH_Y - 3, 80, 11, Component.literal("Search"));
            searchBox.setResponder(s -> {
                currentPage = 0;
                updateFilteredItems();
            });
            this.addRenderableWidget(searchBox);
        } else {
            searchBox.setX(bgX + SEARCH_X);
            searchBox.setY(bgY + SEARCH_Y - 3);
        }
        this.addRenderableWidget(searchBox); // 每次都添加

        // 分页按钮（用 GrayButton 替换）
        this.addRenderableWidget(new GrayButton(bgX + 7, bgY + PAGE_BTN_Y + 10 - 18, PAGE_BTN_WIDTH, PAGE_BTN_HEIGHT,
                Component.literal("<"), b -> {
            if (currentPage > 0) {
                currentPage--;
                updateFilteredItems();
            }
        }));

        this.addRenderableWidget(new GrayButton(bgX + BG_WIDTH - PAGE_BTN_WIDTH - 7, bgY + PAGE_BTN_Y + 10 - 18, PAGE_BTN_WIDTH, PAGE_BTN_HEIGHT,
                Component.literal(">"), b -> {
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateFilteredItems();
            }
        }));

        updateFilteredItems();
    }

    private class TabButton extends Button {
        private final CreativeModeTab tab;
        private final int tabIndex;

        public TabButton(int x, int y, int width, int height, CreativeModeTab tab, int tabIndex) {
            super(x, y, width, height, Component.literal(""), b -> {
                currentTab = tab;
                currentPage = 0;
                updateFilteredItems();
            }, DEFAULT_NARRATION);
            this.tab = tab;
            this.tabIndex = tabIndex;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            boolean selected = (tab == currentTab) || (currentTab == null && nonEmptyTabs.indexOf(tab) == 0);
            int id = System.identityHashCode(this);

            // 动画 alpha 过渡
            float alpha = tabHoverAlpha.getOrDefault(id, selected ? 1f : 0f);
            boolean hovering = mouseX >= this.getX() && mouseX < this.getX() + this.getWidth()
                    && mouseY >= this.getY() && mouseY < this.getY() + this.getHeight();
            if (hovering) alpha += 0.1f;
            else alpha -= 0.1f;
            alpha = Math.max(0f, Math.min(1f, alpha));
            tabHoverAlpha.put(id, alpha);

            int baseColor = selected ? 0xFF6666FF : 0xFF444444;
            int hoverColor = 0xFF8888FF;
            int color = blendColor(baseColor, hoverColor, alpha);
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.getWidth(), this.getY() + this.getHeight(), color);

            ItemStack icon = tab.getDisplayItems().stream().findAny().orElse(ItemStack.EMPTY);
            if (!icon.isEmpty()) guiGraphics.renderItem(icon, this.getX() + 4, this.getY() + 6);
        }
    }

    private int blendColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * ratio);
        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public void updateFilteredItems() {
        if (loading || allItems == null || allItems.isEmpty()) {
            filteredItems = Collections.emptyList();
            totalPages = 1;
            currentPage = 0;
            return;
        }
        if (currentTab == null && !nonEmptyTabs.isEmpty()) currentTab = nonEmptyTabs.get(0);

        String query = searchBox != null ? searchBox.getValue().toLowerCase(Locale.ROOT) : "";
        List<Item> tabItems = currentTab != null
                ? currentTab.getDisplayItems().stream().map(ItemStack::getItem).distinct().toList()
                : allItems;
        if (filteredItems != null && query.equals(lastQuery) && currentTab == lastTab) return;
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
        totalPages = Math.max(1, (filteredItems.size() + COLUMNS * ROWS - 1) / (COLUMNS * ROWS));
        if (currentPage >= totalPages) currentPage = totalPages - 1;
        // 预先标记白名单物品
        for (Item item : filteredItems) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (ClientWhitelist.getWhitelist().contains(id) && !tempEdits.containsKey(id)) {
                tempEdits.put(id, Status.WHITELISTED);
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // 背景
        guiGraphics.fill(bgX - 10, bgY - 30, bgX + BG_WIDTH + 10, bgY + BG_HEIGHT + 10, 0xAA202020);

        // 标题
        String title = this.title.getString();
        guiGraphics.drawString(this.font, title, bgX + BG_WIDTH / 2 - this.font.width(title) / 2, bgY - 45, 0xFFFFFF);

        // 绘制物品格子
        int startX = bgX + ITEMS_X;
        int startY = bgY + ITEMS_Y;
        int startIdx = currentPage * COLUMNS * ROWS;
        int endIdx = Math.min(filteredItems.size(), startIdx + COLUMNS * ROWS);
        int index = startIdx;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                if (index >= endIdx) break;

                Item item = filteredItems.get(index);
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                Status status = tempEdits.get(id);
                if (status == null && ClientWhitelist.getWhitelist().contains(id)) status = Status.WHITELISTED;

                int slotId = index;
                float alpha = slotHoverAlpha.getOrDefault(slotId, 0f);
                boolean hovering = mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE;
                if (hovering) alpha += 0.1f;
                else alpha -= 0.1f;
                alpha = Math.max(0f, Math.min(1f, alpha));
                slotHoverAlpha.put(slotId, alpha);

                // 背景和状态色
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xAA505050);
                if (status != null) {
                    int color = switch (status) {
                        case WHITELISTED -> 0x8033FF33;
                        case TO_ADD -> 0x803366FF;
                        case TO_REMOVE -> 0x80FF3333;
                    };
                    guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, color);
                }
                // hover 过渡高亮
                guiGraphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, blendColor(0x00000000, 0x40FFFFFF, alpha));

                guiGraphics.renderItem(new ItemStack(item), x + 1, y + 1);
                if (hovering) guiGraphics.renderTooltip(this.font, new ItemStack(item), mouseX, mouseY);

                index++;
            }
        }

        // 搜索框
        if (searchBox != null) searchBox.render(guiGraphics, mouseX, mouseY, partialTicks);

        // 分页信息
        String pageInfo = (currentPage + 1) + "/" + totalPages;
        guiGraphics.drawString(this.font, pageInfo, bgX + BG_WIDTH / 2 - this.font.width(pageInfo) / 2, bgY + PAGE_BTN_Y + PAGE_BTN_HEIGHT + 2 - 22, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (searchBox != null && searchBox.isFocused() && searchBox.mouseClicked(mouseX, mouseY, button)) return true;
        if (loading || filteredItems == null) return false;

        int startX = bgX + ITEMS_X;
        int startY = bgY + ITEMS_Y;
        int startIdx = currentPage * COLUMNS * ROWS;
        int endIdx = Math.min(filteredItems.size(), startIdx + COLUMNS * ROWS);
        int index = startIdx;

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                int x = startX + col * SLOT_SIZE;
                int y = startY + row * SLOT_SIZE;
                if (index >= endIdx) break;

                if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE) {
                    Item item = filteredItems.get(index);
                    ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                    if (button == 0) { // 左键获取物品
                        ModMessages.getChannel().sendToServer(new C2SRequestItemPacket(id));
                    } else if (button == 2 && Minecraft.getInstance().player != null && Minecraft.getInstance().player.hasPermissions(2)) {
                        // 中键开始拖拽标记
                        isDragging = true;
                        toggleSlot(id);
                        draggingSlots.add(index);
                    }
                    return true;
                }
                index++;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 2) {
            isDragging = false;
            draggingSlots.clear();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (isDragging && button == 2 && filteredItems != null) {
            int startX = bgX + ITEMS_X;
            int startY = bgY + ITEMS_Y;
            int startIdx = currentPage * COLUMNS * ROWS;
            int endIdx = Math.min(filteredItems.size(), startIdx + COLUMNS * ROWS);
            int index = startIdx;

            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLUMNS; col++) {
                    int x = startX + col * SLOT_SIZE;
                    int y = startY + row * SLOT_SIZE;
                    if (index >= endIdx) break;

                    if (mouseX >= x && mouseX < x + SLOT_SIZE && mouseY >= y && mouseY < y + SLOT_SIZE && !draggingSlots.contains(index)) {
                        Item item = filteredItems.get(index);
                        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                        toggleSlot(id);
                        draggingSlots.add(index);
                    }
                    index++;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    // 辅助方法：切换白名单状态
    private void toggleSlot(ResourceLocation id) {
        Status current = tempEdits.get(id);
        boolean inWhitelist = ClientWhitelist.getWhitelist().contains(id);
        if (current == null) {
            tempEdits.put(id, inWhitelist ? Status.TO_REMOVE : Status.TO_ADD);
        } else if (current == Status.TO_ADD) {
            tempEdits.remove(id); // 取消待添加，回到未白名单
        } else if (current == Status.TO_REMOVE) {
            tempEdits.remove(id); // 取消待移除，回到白名单
        }

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
    private class GrayButton extends Button {
        public GrayButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            int color = 0xAA444444; // 半透明深灰
            if (this.isHoveredOrFocused()) {
                color = 0xCC666666; // hover 时更亮
            }
            guiGraphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), color);
            int textColor = 0xFFFFFF; // 白色
            guiGraphics.drawCenteredString(font, getMessage(), getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, textColor);
        }
    }
}
