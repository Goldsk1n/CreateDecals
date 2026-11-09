package com.goldskinmc.createdecals.client;

import com.goldskinmc.createdecals.CreateDecals;
import com.goldskinmc.createdecals.decal.DecalText;
import com.goldskinmc.createdecals.registry.DecalsComponents;
import com.goldskinmc.createdecals.stencil.DecalStencilItem;
import com.goldskinmc.createdecals.stencil.StencilData;
import com.goldskinmc.createdecals.stencil.StencilPayload;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.createmod.catnip.gui.element.ScreenElement;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The Decal Stencil's screen, built as Create's own screens are (its schematic screens: a grey header strip, a blue
 * two-tone body, sunken slots, Create's input bars, a grey tray with the confirm tab sticking out past its right edge;
 * {@code textures/gui/stencil.png}, drawn with its slots where the layout constants below put
 * things). Categories are Create's buttons with a small icon each, the chosen one pressed; the decals of one category
 * fill the grid (scroll for more); a field in the tray types a word instead; the 16 dye colours and a hex field; Create's
 * icon buttons to turn, flip, erase and finish, toggles for the layer (behind or in front of the decal already on a
 * face) and glow; a preview of the decal as it will lie on a face; and the stencil itself drawn large beside the panel,
 * as Create shows its blocks. Closing it stores the choice on the stencil (one serverbound payload).
 */
public class StencilScreen extends Screen {
    private static final ResourceLocation BACKGROUND = CreateDecals.id("textures/gui/stencil.png");
    private static final List<String> CATEGORY_ORDER = List.of("letters", "digits", "signs", "lines", "markers", "emblems");
    private static final int PANEL_WIDTH = 290;
    private static final int PANEL_HEIGHT = 192;
    /** The background is drawn a little wider than the panel: the confirm tab sticks out past its right edge. */
    private static final int BACKGROUND_WIDTH = 300;
    private static final int COLUMNS = 9;
    private static final int ROWS = 4;
    private static final int CELL = 22;
    private static final int GRID_X = 6;
    private static final int GRID_Y = 40;
    private static final int TABS_X = 6;
    private static final int TABS_Y = 19;
    private static final int TAB = 18;
    private static final int TAB_STEP = 21;
    private static final int TAB_ARROW = 12;
    /** Category buttons in a row before it has to scroll. */
    private static final int MAX_TABS = 7;
    private static final int PREVIEW_X = 214;
    private static final int PREVIEW_Y = 40;
    /** The light block face inside the preview's frame. */
    private static final int FACE_X = PREVIEW_X + 3;
    private static final int FACE_Y = PREVIEW_Y + 3;
    private static final int FACE = 64;
    private static final int BUTTONS_Y = 114;
    private static final int SWATCH = 13;
    private static final int SWATCH_X = 6;
    private static final int SWATCH_Y = 134;
    private static final int TRAY_Y = 164;
    private static final int ICONS_V = 200;
    private static final int TITLE_COLOUR = 0x404040;
    private static final int SELECTED = 0xFFE4B763;
    private static final int SELECTED_LIGHT = 0xFFFFEB8C;

    private final InteractionHand hand;
    private final ItemStack stack;
    @Nullable
    private final Component wrongBlock;
    private StencilData data;
    private final Map<String, List<DecalLibrary.Entry>> categories = new LinkedHashMap<>();
    private final List<String> categoryNames = new ArrayList<>();
    /** The category buttons shown now: x, y, width, height, category index. */
    private final List<int[]> tabBounds = new ArrayList<>();
    private int category;
    /** The first category button shown, when resource packs bring more categories than fit the row. */
    private int firstTab;
    private boolean tabsOverflow;
    private int page;
    private EditBox hex;
    private EditBox text;
    private IconButton layer;
    private IconButton glow;
    private IconButton flip;
    private int left;
    private int top;

    public StencilScreen(InteractionHand hand, ItemStack stack) {
        this(hand, stack, null);
    }

    /** {@code wrongBlock}: the name of a block that takes no decals, when the screen opened from a click on one. */
    public StencilScreen(InteractionHand hand, ItemStack stack, @Nullable Component wrongBlock) {
        super(Component.translatable("createdecals.stencil.title"));
        this.hand = hand;
        this.stack = stack.copy();
        this.wrongBlock = wrongBlock;
        this.data = DecalStencilItem.data(stack);
        List<DecalLibrary.Entry> all = new ArrayList<>(DecalLibrary.INSTANCE.all());
        all.sort(Comparator.comparing((DecalLibrary.Entry e) -> categoryRank(e.category())).thenComparing(e -> e.id().toString()));
        for (DecalLibrary.Entry entry : all) {
            categories.computeIfAbsent(entry.category(), k -> new ArrayList<>()).add(entry);
        }
        categoryNames.addAll(categories.keySet());
        data.decal().flatMap(id -> all.stream().filter(e -> e.id().equals(id)).findFirst())
                .ifPresent(e -> category = Math.max(0, categoryNames.indexOf(e.category())));
    }

    private static int categoryRank(String name) {
        int index = CATEGORY_ORDER.indexOf(name);
        return index >= 0 ? index : CATEGORY_ORDER.size();
    }

    @Override
    protected void init() {
        left = (width - PANEL_WIDTH) / 2;
        top = (height - PANEL_HEIGHT) / 2;
        layoutTabs();
        int bx = left + PREVIEW_X;
        int by = top + BUTTONS_Y;
        button(bx, by, AllIcons.I_TOOL_ROTATE, "rotate", () -> data = data.withTurn(data.rotation() + 1, data.flip()));
        flip = button(bx + 20, by, AllIcons.I_TOOL_MIRROR, "flip", () -> data = data.withTurn(data.rotation(), !data.flip()));
        button(bx + 40, by, AllIcons.I_TRASH, "erase", () -> data = data.withErase(!data.erase()));
        button(left + PANEL_WIDTH - 25, top + TRAY_Y + 5, AllIcons.I_CONFIRM, "done", this::onClose);
        layer = new IconButton(bx, by + 20, icon(0));
        layer.withCallback(() -> {
            data = data.withFront(!data.front());
            showToggles();
        });
        addRenderableWidget(layer);
        glow = new IconButton(bx + 20, by + 20, icon(2));
        glow.withCallback(() -> {
            data = data.withGlow(!data.glow());
            showToggles();
        });
        addRenderableWidget(glow);
        showToggles();
        // The word field sits in the tray's input bar (40-236 across, 171-184 down), drawn by the background
        text = new EditBox(font, left + 44, top + TRAY_Y + 10, 190, 10, Component.translatable("createdecals.stencil.text"));
        text.setBordered(false);
        text.setTextColor(0xFFFFFF);
        text.setMaxLength(DecalText.MAX_LENGTH);
        text.setFilter(value -> value.chars().allMatch(c -> DecalText.allowed((char) c)));
        text.setHint(Component.translatable("createdecals.stencil.text_hint").withStyle(ChatFormatting.GRAY));
        text.setValue(data.text());
        text.setResponder(value -> {
            // Words are laid in capitals: show them so as they are typed
            String upper = value.toUpperCase(Locale.ROOT);
            if (!upper.equals(value)) {
                text.setValue(upper);
                return;
            }
            data = data.withText(value);
        });
        addRenderableWidget(text);
        // The hex field sits in the body's input bar (120-200 across, 140-153 down)
        hex = new EditBox(font, left + 124, top + 143, 74, 10, Component.translatable("createdecals.stencil.colour"));
        hex.setBordered(false);
        hex.setTextColor(0xFFFFFF);
        hex.setMaxLength(7);
        hex.setValue(String.format(Locale.ROOT, "#%06X", data.rgb()));
        hex.setResponder(text -> {
            String digits = text.startsWith("#") ? text.substring(1) : text;
            if (digits.length() == 6) {
                try {
                    data = data.withRgb(Integer.parseInt(digits, 16));
                } catch (NumberFormatException ignored) {
                    // not a colour yet: keep the last one
                }
            }
        });
        addRenderableWidget(hex);
    }

    /** One of the screen's own 16 px icons, drawn on Create's button: 0 back layer, 1 front layer, 2 glow. */
    private static ScreenElement icon(int index) {
        return (graphics, x, y) -> graphics.blit(BACKGROUND, x, y, 16 * index, ICONS_V, 16, 16, 512, 256);
    }

    /** The layer button shows the layer chosen; the glow button turns green while on. */
    private void showToggles() {
        layer.setIcon(icon(data.front() ? 1 : 0));
        layer.setToolTip(Component.translatable(data.front() ? "createdecals.stencil.layer_front" : "createdecals.stencil.layer_back"));
        glow.green = data.glow();
        glow.setToolTip(Component.translatable(data.glow() ? "createdecals.stencil.glow_on" : "createdecals.stencil.glow_off"));
    }

    private IconButton button(int x, int y, AllIcons icon, String key, Runnable action) {
        IconButton button = new IconButton(x, y, icon);
        button.setToolTip(Component.translatable("createdecals.stencil." + key));
        button.withCallback(action);
        addRenderableWidget(button);
        return button;
    }

    /**
     * A decal's name for its tooltip: {@code decal.<namespace>.<path>} from the language files (resource packs can name
     * theirs), else made from its id: words capitalised, a size suffix shown as the size.
     */
    static Component decalName(DecalLibrary.Entry entry) {
        String[] words = entry.id().getPath().split("_");
        StringBuilder name = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty() || word.matches("\\dx\\d")) {
                continue;
            }
            if (!name.isEmpty()) {
                name.append(' ');
            }
            name.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        if (entry.width() > 1 || entry.height() > 1) {
            name.append(" (").append(entry.width()).append(" x ").append(entry.height()).append(')');
        }
        return Component.translatableWithFallback("decal." + entry.id().getNamespace() + "." + entry.id().getPath(), name.toString());
    }

    /**
     * Lays the category buttons out from {@link #firstTab}. When there are more than fit (resource packs can add
     * categories), the row scrolls: two arrow buttons after it, the mouse wheel over it, and the chosen category kept in
     * view.
     */
    private void layoutTabs() {
        tabsOverflow = categoryNames.size() > MAX_TABS;
        int shown = tabsOverflow ? MAX_TABS - 1 : categoryNames.size();
        firstTab = Math.max(0, Math.min(firstTab, categoryNames.size() - shown));
        if (category < firstTab) {
            firstTab = category;
        } else if (category >= firstTab + shown) {
            firstTab = category - shown + 1;
        }
        tabBounds.clear();
        for (int i = 0; i < shown && firstTab + i < categoryNames.size(); i++) {
            tabBounds.add(new int[] {left + TABS_X + i * TAB_STEP, top + TABS_Y, TAB, TAB, firstTab + i});
        }
    }

    private void scrollTabs(int by) {
        int shown = tabsOverflow ? MAX_TABS - 1 : categoryNames.size();
        firstTab = Math.max(0, Math.min(categoryNames.size() - shown, firstTab + by));
        // Scrolling moves the row, not the choice
        tabBounds.clear();
        for (int i = 0; i < shown && firstTab + i < categoryNames.size(); i++) {
            tabBounds.add(new int[] {left + TABS_X + i * TAB_STEP, top + TABS_Y, TAB, TAB, firstTab + i});
        }
    }

    private static Component categoryLabel(String name) {
        return Component.translatableWithFallback("createdecals.category." + name, name);
    }

    private List<DecalLibrary.Entry> categoryEntries() {
        return categoryNames.isEmpty() ? List.of() : categories.get(categoryNames.get(category));
    }

    private List<DecalLibrary.Entry> shown() {
        List<DecalLibrary.Entry> entries = categoryEntries();
        int from = Math.min(page * COLUMNS * ROWS, entries.size());
        return entries.subList(from, Math.min(from + COLUMNS * ROWS, entries.size()));
    }

    private int pages() {
        return Math.max(1, (categoryEntries().size() + COLUMNS * ROWS - 1) / (COLUMNS * ROWS));
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.blit(BACKGROUND, left, top, 0, 0, BACKGROUND_WIDTH, PANEL_HEIGHT, 512, 256);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        flip.active = !data.writes();
        flip.setToolTip(Component.translatable(data.writes() ? "createdecals.stencil.flip_words" : "createdecals.stencil.flip"));
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(font, title, left + (PANEL_WIDTH - font.width(title)) / 2, top + 4, TITLE_COLOUR, false);
        String hoveredCategory = renderTabs(graphics, mouseX, mouseY);
        List<DecalLibrary.Entry> entries = shown();
        DecalLibrary.Entry hoveredEntry = null;
        for (int i = 0; i < entries.size(); i++) {
            DecalLibrary.Entry entry = entries.get(i);
            int x = left + GRID_X + (i % COLUMNS) * CELL;
            int y = top + GRID_Y + (i / COLUMNS) * CELL;
            boolean selected = !data.erase() && !data.writes() && data.decal().map(entry.id()::equals).orElse(false);
            boolean hovered = mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL;
            drawDecal(graphics, entry, x + 3, y + 3, CELL - 6, 0, false);
            if (hovered) {
                // As a hovered inventory slot: a light wash over it
                graphics.fill(x + 1, y + 1, x + CELL - 1, y + CELL - 1, 0x60FFFFFF);
                hoveredEntry = entry;
            }
            if (selected) {
                frame(graphics, x - 1, y - 1, x + CELL, y + CELL);
            }
        }
        if (pages() > 1) {
            String pageLabel = (page + 1) + "/" + pages();
            graphics.drawString(font, pageLabel, left + PREVIEW_X - 4 - font.width(pageLabel), top + TABS_Y + 5, 0xFFFFFF, true);
        }
        graphics.drawString(font, Component.translatable("createdecals.stencil.text"), left + 8, top + TRAY_Y + 10, TITLE_COLOUR, false);
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < dyes.length; i++) {
            int x = left + SWATCH_X + (i % 8) * SWATCH;
            int y = top + SWATCH_Y + (i / 8) * SWATCH;
            int rgb = dyes[i].getTextureDiffuseColor() & 0xFFFFFF;
            graphics.fill(x + 2, y + 2, x + SWATCH - 2, y + SWATCH - 2, 0xFF000000 | rgb);
            if (rgb == data.rgb()) {
                frame(graphics, x - 1, y - 1, x + SWATCH, y + SWATCH);
            }
        }
        // The preview: the decal as it will lie on a face, turned and flipped
        int fx = left + FACE_X;
        int fy = top + FACE_Y;
        if (data.erase()) {
            graphics.drawCenteredString(font, Component.translatable("createdecals.stencil.erase"), fx + FACE / 2, fy + FACE / 2 - 4, 0xFFFFFF);
        } else if (data.writes()) {
            // The word's letters in a row (the first few if it is long), turned as a whole, as it will lie (never mirrored)
            String word = data.text();
            int shownLetters = Math.min(word.length(), 6);
            int size = Math.min(24, (FACE - 6) / Math.max(1, shownLetters));
            graphics.pose().pushPose();
            graphics.pose().translate(fx + FACE / 2f, fy + FACE / 2f, 0);
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(90 * data.rotation()));
            for (int i = 0; i < shownLetters; i++) {
                ResourceLocation id = DecalText.decalFor(word.charAt(i));
                DecalLibrary.Entry entry = id != null ? DecalLibrary.INSTANCE.get(id) : null;
                if (entry != null) {
                    drawDecal(graphics, entry, -size * shownLetters / 2 + i * size, -size / 2, size, 0, false);
                }
            }
            graphics.pose().popPose();
            sizeLabel(graphics, fx, fy, data.rotation() % 2 == 0 ? word.length() + " x 1" : "1 x " + word.length());
        } else {
            data.decal().map(DecalLibrary.INSTANCE::get).ifPresent(entry -> {
                drawDecal(graphics, entry, fx, fy, FACE, data.rotation(), data.flip());
                if (data.width() > 1 || data.height() > 1) {
                    boolean across = data.rotation() % 2 == 0;
                    sizeLabel(graphics, fx, fy, (across ? data.width() : data.height()) + " x " + (across ? data.height() : data.width()));
                }
            });
        }
        renderStencil(graphics);
        if (wrongBlock != null) {
            wrongBlockNote(graphics);
        }
        if (hoveredEntry != null) {
            graphics.renderTooltip(font, decalName(hoveredEntry), mouseX, mouseY);
        } else if (hoveredCategory != null) {
            graphics.renderTooltip(font, categoryLabel(hoveredCategory), mouseX, mouseY);
        }
    }

    /**
     * The category buttons: Create's own button, pressed for the chosen category and lit under the mouse, with the
     * category's icon (ours for the six built in; a resource pack's category shows its first decal), the chosen one's
     * name beside the row, and the row's arrows when it scrolls. Returns the category under the mouse, for its tooltip.
     */
    @Nullable
    private String renderTabs(GuiGraphics graphics, int mouseX, int mouseY) {
        String hovered = null;
        int end = left + TABS_X;
        for (int[] b : tabBounds) {
            boolean over = mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3];
            (b[4] == category ? AllGuiTextures.BUTTON_DOWN : over ? AllGuiTextures.BUTTON_HOVER : AllGuiTextures.BUTTON)
                    .render(graphics, b[0], b[1]);
            String name = categoryNames.get(b[4]);
            int builtIn = CATEGORY_ORDER.indexOf(name);
            if (builtIn >= 0) {
                graphics.blit(BACKGROUND, b[0] + 1, b[1] + 1, 48 + 16 * builtIn, ICONS_V, 16, 16, 512, 256);
            } else {
                List<DecalLibrary.Entry> entries = categories.get(name);
                if (!entries.isEmpty()) {
                    drawDecal(graphics, entries.get(0), b[0] + 3, b[1] + 3, 12, 0, false, 0xFFFFFF);
                }
            }
            if (over) {
                hovered = name;
            }
            end = b[0] + b[2];
        }
        if (tabsOverflow) {
            for (int arrow = 0; arrow < 2; arrow++) {
                int ax = tabArrowX(arrow);
                int ay = top + TABS_Y;
                boolean over = mouseX >= ax && mouseX < ax + TAB_ARROW && mouseY >= ay && mouseY < ay + TAB;
                graphics.fill(ax, ay, ax + TAB_ARROW, ay + TAB, 0xFF000000);
                graphics.fill(ax, ay, ax + TAB_ARROW - 1, ay + TAB - 1, 0xFFFFFFFF);
                graphics.fill(ax + 1, ay + 1, ax + TAB_ARROW - 1, ay + TAB - 1, over ? 0xFF8892C9 : 0xFF8B8B8B);
                graphics.drawCenteredString(font, arrow == 0 ? "<" : ">", ax + TAB_ARROW / 2, ay + 5, 0xFFFFFF);
            }
        } else if (!categoryNames.isEmpty()) {
            graphics.drawString(font, categoryLabel(categoryNames.get(category)), end + 6, top + TABS_Y + 5, 0xFFFFFF, true);
        }
        return hovered;
    }

    /** The brass frame round a chosen decal or colour, two pixels, lit on its inner edge. */
    private static void frame(GuiGraphics graphics, int x0, int y0, int x1, int y1) {
        graphics.renderOutline(x0, y0, x1 - x0 + 1, y1 - y0 + 1, SELECTED);
        graphics.renderOutline(x0 + 1, y0 + 1, x1 - x0 - 1, y1 - y0 - 1, SELECTED_LIGHT);
    }

    /** The stencil, large, beside the panel's lower right corner, as Create shows its blocks; left out on narrow screens. */
    private void renderStencil(GuiGraphics graphics) {
        int x = left + BACKGROUND_WIDTH + 6;
        if (x + 40 > width) {
            return;
        }
        graphics.pose().pushPose();
        graphics.pose().translate(x, top + PANEL_HEIGHT - 56, 100);
        graphics.pose().scale(2.5f, 2.5f, 1);
        // The icon shows the decal the stencil holds: what is chosen now, not what it held when the screen opened
        stack.set(DecalsComponents.STENCIL.get(), data);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }

    /**
     * Above the panel, when the screen opened from a click on a block that takes no decals: where decals go, in brass and
     * dark grey, as wide as its text and centred on the panel, wrapped only if the window is narrower (over the panel's
     * title bar if the window leaves no room above it).
     */
    private void wrongBlockNote(GuiGraphics graphics) {
        Component note = Component.translatable("createdecals.stencil.wrong_block", wrongBlock);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(note, Math.max(PANEL_WIDTH - 12, width - 20));
        int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        int boxWidth = textWidth + 12;
        int x = Math.max(4, left + (PANEL_WIDTH - boxWidth) / 2);
        int boxHeight = lines.size() * 10 + 6;
        int y = Math.max(2, top - boxHeight - 3);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 300);
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xFFB8763E);
        graphics.fill(x + 1, y + 1, x + boxWidth - 1, y + boxHeight - 1, 0xF0303438);
        for (int i = 0; i < lines.size(); i++) {
            graphics.drawString(font, lines.get(i), x + 6, y + 4 + i * 10, 0xF7CB6C, false);
        }
        graphics.pose().popPose();
    }

    /** How many blocks across and down, in the preview face's bottom right corner. */
    private void sizeLabel(GuiGraphics graphics, int fx, int fy, String label) {
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 200);
        graphics.drawString(font, label, fx + FACE - 2 - font.width(label), fy + FACE - 10, 0xFFFFFF);
        graphics.pose().popPose();
    }

    /** The decal's whole sprite (all tiles of a large one), tinted, flipped and then turned {@code rotation} quarters. */
    private void drawDecal(GuiGraphics graphics, DecalLibrary.Entry entry, int x, int y, int size, int rotation, boolean flip) {
        drawDecal(graphics, entry, x, y, size, rotation, flip, data.rgb());
    }

    private void drawDecal(GuiGraphics graphics, DecalLibrary.Entry entry, int x, int y, int size, int rotation, boolean flip, int rgb) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(entry.sprite());
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        graphics.pose().pushPose();
        graphics.pose().translate(x + size / 2f, y + size / 2f, 0);
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(90 * rotation));
        if (flip) {
            graphics.pose().scale(-1, 1, 1);
            // A mirrored quad winds the other way, and the GUI culls back faces: without this it vanished
            RenderSystem.disableCull();
        }
        blitInset(graphics, sprite, -size / 2, -size / 2, size, r, g, b);
        if (flip) {
            RenderSystem.enableCull();
        }
        graphics.pose().popPose();
    }

    /**
     * {@code GuiGraphics.blit} with a sprite, its texture coordinates pulled in by a fiftieth of a texel: at GUI scales
     * that put a decal's edge between screen pixels (a word's letters at scale 3), the plain blit sampled the next
     * sprite on the block atlas and drew a line of it beside the letter.
     */
    private static void blitInset(GuiGraphics graphics, TextureAtlasSprite sprite, int x, int y, int size, float r, float g, float b) {
        float du = (sprite.getU1() - sprite.getU0()) / sprite.contents().width() * 0.02f;
        float dv = (sprite.getV1() - sprite.getV0()) / sprite.contents().height() * 0.02f;
        float u0 = sprite.getU0() + du;
        float u1 = sprite.getU1() - du;
        float v0 = sprite.getV0() + dv;
        float v1 = sprite.getV1() - dv;
        RenderSystem.setShaderTexture(0, sprite.atlasLocation());
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f pose = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        buffer.addVertex(pose, x, y, 0).setUv(u0, v0).setColor(r, g, b, 1f);
        buffer.addVertex(pose, x, y + size, 0).setUv(u0, v1).setColor(r, g, b, 1f);
        buffer.addVertex(pose, x + size, y + size, 0).setUv(u1, v1).setColor(r, g, b, 1f);
        buffer.addVertex(pose, x + size, y, 0).setUv(u1, v0).setColor(r, g, b, 1f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.disableBlend();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int[] b : tabBounds) {
            if (mouseX >= b[0] && mouseX < b[0] + b[2] && mouseY >= b[1] && mouseY < b[1] + b[3]) {
                category = b[4];
                page = 0;
                return true;
            }
        }
        if (tabsOverflow && mouseY >= top + TABS_Y && mouseY < top + TABS_Y + TAB) {
            for (int arrow = 0; arrow < 2; arrow++) {
                if (mouseX >= tabArrowX(arrow) && mouseX < tabArrowX(arrow) + TAB_ARROW) {
                    scrollTabs(arrow == 0 ? -1 : 1);
                    return true;
                }
            }
        }
        List<DecalLibrary.Entry> entries = shown();
        for (int i = 0; i < entries.size(); i++) {
            double[] c = cellCentre(i);
            if (Math.abs(mouseX - c[0]) < CELL / 2.0 && Math.abs(mouseY - c[1]) < CELL / 2.0) {
                DecalLibrary.Entry entry = entries.get(i);
                data = data.withDecal(entry.id(), entry.width(), entry.height());
                text.setValue("");
                return true;
            }
        }
        for (DyeColor dye : DyeColor.values()) {
            double[] c = swatchCentre(dye);
            if (Math.abs(mouseX - c[0]) <= SWATCH / 2.0 && Math.abs(mouseY - c[1]) <= SWATCH / 2.0) {
                data = data.withRgb(dye.getTextureDiffuseColor());
                hex.setValue(String.format(Locale.ROOT, "#%06X", data.rgb()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (tabsOverflow && mouseY >= top + TABS_Y && mouseY < top + TABS_Y + TAB) {
            scrollTabs(-(int) Math.signum(scrollY));
            return true;
        }
        page = Math.max(0, Math.min(pages() - 1, page - (int) Math.signum(scrollY)));
        return true;
    }

    private int tabArrowX(int arrow) {
        return left + TABS_X + (MAX_TABS - 1) * TAB_STEP + arrow * (TAB_ARROW + 2);
    }

    /** For automated checks: the categories and which buttons are shown (category indices). */
    public List<String> categoryNames() {
        return categoryNames;
    }

    public List<Integer> shownTabs() {
        return tabBounds.stream().map(b -> b[4]).toList();
    }

    /** For automated checks: clicks a category-row arrow (0 back, 1 on) as a player would. */
    public void clickTabArrow(int arrow) {
        mouseClicked(tabArrowX(arrow) + TAB_ARROW / 2.0, top + TABS_Y + TAB / 2.0, 0);
    }

    @Override
    public void onClose() {
        PacketDistributor.sendToServer(new StencilPayload(hand, data));
        super.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** For automated checks: what the screen holds now. */
    public StencilData data() {
        return data;
    }

    /** For automated checks, which click like a player: the centre of the {@code i}th decal cell shown. */
    public double[] cellCentre(int i) {
        return new double[] {left + GRID_X + (i % COLUMNS) * CELL + CELL / 2.0, top + GRID_Y + (i / COLUMNS) * CELL + CELL / 2.0};
    }

    /** For automated checks: the centre of the word field, the layer button and the glow button. */
    public double[] textCentre() {
        return new double[] {text.getX() + text.getWidth() / 2.0, text.getY() + text.getHeight() / 2.0};
    }

    public double[] layerCentre() {
        return new double[] {layer.getX() + 9, layer.getY() + 9};
    }

    public double[] glowCentre() {
        return new double[] {glow.getX() + 9, glow.getY() + 9};
    }

    /** For automated checks: the centre of the swatch of {@code dye}. */
    public double[] swatchCentre(DyeColor dye) {
        int i = dye.getId();
        return new double[] {left + SWATCH_X + (i % 8) * SWATCH + SWATCH / 2.0, top + SWATCH_Y + (i / 8) * SWATCH + SWATCH / 2.0};
    }
}
