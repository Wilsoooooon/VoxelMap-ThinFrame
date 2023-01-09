package com.mamiyaotaru.voxelmap.gui.overridden;

import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;

public abstract class PopupGuiScreen extends GuiScreenMinimap implements IPopupGuiScreen {
    private final ArrayList<Popup> popups = new ArrayList<>();

    @Override
    public void removed() {}

    public void createPopup(int x, int y, int directX, int directY, ArrayList<Popup.PopupEntry> entries) { popups.add(new Popup(x, y, directX, directY, entries, this)); }

    public void clearPopups() { popups.clear(); }

    public boolean clickedPopup(double x, double y) {
        boolean clicked = false;
        ArrayList<Popup> deadPopups = new ArrayList<>();

        for (Popup popup : this.popups) {
            boolean clickedPopup = popup.clickedMe(x, y);
            if (!clickedPopup) {
                deadPopups.add(popup);
            } else if (popup.shouldClose()) {
                deadPopups.add(popup);
            }

            clicked = clicked || clickedPopup;
        }

        this.popups.removeAll(deadPopups);
        return clicked;
    }

    @Override
    public boolean overPopup(int mouseX, int mouseY) {
        boolean over = false;

        for (Popup popup : this.popups) {
            boolean overPopup = popup.overMe(mouseX, mouseY);
            over = over || overPopup;
        }

        return !over;
    }

    @Override
    public boolean popupOpen() { return popups.isEmpty(); }

    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        super.render(matrices, mouseX, mouseY, delta);

        for (Popup popup : this.popups) {
            popup.drawPopup(matrices, mouseX, mouseY);
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return !this.clickedPopup(mouseX, mouseY) && super.mouseClicked(mouseX, mouseY, button);
    }
}
