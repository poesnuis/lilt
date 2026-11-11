package dev.lilt.config;

public final class Appearance {
    public double scale = 1;
    public double opacity = .92;
    public int padding = 8;
    public int radius = 6;
    public int borderWidth = 1;
    public int spacing = 3;
    public int fontSize = 11;
    public String font = "Open Sans";
    public boolean semibold = false;
    public String align = "LEFT";
    public boolean background = true;
    public boolean animate = true;
    public double animationSpeed = 1;
    public String foreground = "EAF5F9";
    public String backgroundColor = "182B36";
    public String borderColor = "385665";
    public String accent = "67D1F5";

    public void validate() {
        scale = Config.clamp(scale, .5, 3, 1);
        opacity = Config.clamp(opacity, .1, 1, .92);
        padding = Config.clamp(padding, 0, 24);
        radius = Config.clamp(radius, 0, 20);
        borderWidth = Config.clamp(borderWidth, 0, 4);
        spacing = Config.clamp(spacing, 0, 16);
        fontSize = Config.clamp(fontSize, 8, 24);
        animationSpeed = Config.clamp(animationSpeed, .25, 3, 1);
        if (!"Minecraft".equals(font)) font = "Open Sans";
        if (!"CENTER".equals(align) && !"RIGHT".equals(align)) align = "LEFT";
        foreground = Config.hex(foreground, "EAF5F9");
        backgroundColor = Config.hex(backgroundColor, "182B36");
        borderColor = Config.hex(borderColor, "385665");
        accent = Config.hex(accent, "67D1F5");
    }
    public int color(String hex) { return 0xff000000 | Integer.parseInt(hex, 16); }
}
