import os
import sys
from PIL import Image, ImageDraw, ImageFilter
import numpy as np

def generate_icons():
    src_path = r"C:\Users\DEll\.gemini\antigravity\brain\1d799bd3-711d-4766-8e8b-1f3a9d19c0e4\icon_direction_mechanical_1791150083766.jpg"
    res_dir = r"c:\Users\DEll\Documents\antigravity\blockit\app\src\main\res"

    print("Loading source image:", src_path)
    src = Image.open(src_path).convert("RGBA")

    # Center and radius of the titanium dial in source
    cx, cy = 508, 490
    dial_radius = 392

    # 1. Supersampled circular crop of the mechanical dial
    mask_hi = Image.new("L", (2048, 2048), 0)
    draw_hi = ImageDraw.Draw(mask_hi)
    draw_hi.ellipse(
        [cx * 2 - dial_radius * 2, cy * 2 - dial_radius * 2,
         cx * 2 + dial_radius * 2, cy * 2 + dial_radius * 2],
        fill=255
    )
    mask = mask_hi.resize((1024, 1024), Image.Resampling.LANCZOS)

    dial_box = (cx - dial_radius, cy - dial_radius, cx + dial_radius, cy + dial_radius)
    dial_rgba = Image.new("RGBA", (dial_radius * 2, dial_radius * 2), (0, 0, 0, 0))
    dial_rgba.paste(src.crop(dial_box), (0, 0), mask=mask.crop(dial_box))

    # 2. Master Background Layer (1024x1024)
    # Bead-blasted space-gray titanium slate texture with radial lighting
    y, x = np.ogrid[:1024, :1024]
    dist_from_center = np.sqrt((x - 512)**2 + (y - 512)**2) / (512 * np.sqrt(2))
    grad = np.clip(1.0 - dist_from_center * 0.45, 0.45, 1.0)

    # Base dark metal color: (34, 37, 42) to (18, 20, 23)
    np.random.seed(42)
    noise = np.random.normal(0, 2.5, (1024, 1024))
    r = np.clip(32 * grad + noise, 0, 255).astype(np.uint8)
    g = np.clip(35 * grad + noise, 0, 255).astype(np.uint8)
    b = np.clip(40 * grad + noise, 0, 255).astype(np.uint8)

    bg_arr = np.dstack((r, g, b, np.full((1024, 1024), 255, dtype=np.uint8)))
    master_bg = Image.fromarray(bg_arr, "RGBA").filter(ImageFilter.GaussianBlur(radius=0.5))

    # 3. Master Foreground Layer (1024x1024)
    # Scale dial to fit with comfortable margin inside 72dp safe zone viewport (66.67%)
    # Using 608px (59.3% of 1024) ensures dial never clips even with aggressive squircle/circle masks
    dial_size = int(1024 * 0.593)
    dial_scaled = dial_rgba.resize((dial_size, dial_size), Image.Resampling.LANCZOS)

    # Create soft drop shadow for physical tactile depth
    shadow_margin = 40
    shadow_size = dial_size + shadow_margin
    shadow = Image.new("RGBA", (shadow_size, shadow_size), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(shadow)
    s_offset = shadow_margin // 2
    s_draw.ellipse(
        [s_offset, s_offset + 6, s_offset + dial_size, s_offset + dial_size + 6],
        fill=(0, 0, 0, 160)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=10))

    master_fg = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    fg_x = (1024 - dial_size) // 2
    fg_y = (1024 - dial_size) // 2
    master_fg.paste(shadow, (fg_x - s_offset, fg_y - s_offset), mask=shadow)
    master_fg.paste(dial_scaled, (fg_x, fg_y), mask=dial_scaled)

    # 4. Master Legacy Squircle Icon (1024x1024)
    squircle_mask = Image.new("L", (1024, 1024), 0)
    draw_sq = ImageDraw.Draw(squircle_mask)
    sq_box = [52, 52, 972, 972]
    draw_sq.rounded_rectangle(sq_box, radius=180, fill=255)

    master_legacy_sq = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    master_legacy_sq.paste(master_bg, (0, 0), mask=squircle_mask)
    master_legacy_sq.paste(master_fg, (0, 0), mask=master_fg)
    draw_sq_rim = ImageDraw.Draw(master_legacy_sq)
    draw_sq_rim.rounded_rectangle(sq_box, radius=180, outline=(255, 255, 255, 28), width=3)

    # 5. Master Legacy Round Icon (1024x1024)
    round_mask = Image.new("L", (1024, 1024), 0)
    draw_rd = ImageDraw.Draw(round_mask)
    rd_box = [52, 52, 972, 972]
    draw_rd.ellipse(rd_box, fill=255)

    master_legacy_rd = Image.new("RGBA", (1024, 1024), (0, 0, 0, 0))
    master_legacy_rd.paste(master_bg, (0, 0), mask=round_mask)
    master_legacy_rd.paste(master_fg, (0, 0), mask=master_fg)
    draw_rd_rim = ImageDraw.Draw(master_legacy_rd)
    draw_rd_rim.ellipse(rd_box, outline=(255, 255, 255, 28), width=3)

    # Density configurations
    densities = {
        "mipmap-mdpi": {"launcher": 48, "adaptive": 108},
        "mipmap-hdpi": {"launcher": 72, "adaptive": 162},
        "mipmap-xhdpi": {"launcher": 96, "adaptive": 216},
        "mipmap-xxhdpi": {"launcher": 144, "adaptive": 324},
        "mipmap-xxxhdpi": {"launcher": 192, "adaptive": 432},
    }

    for folder_name, sizes in densities.items():
        folder_path = os.path.join(res_dir, folder_name)
        os.makedirs(folder_path, exist_ok=True)

        l_sz = sizes["launcher"]
        a_sz = sizes["adaptive"]

        # 1. ic_launcher.png
        icon_sq = master_legacy_sq.resize((l_sz, l_sz), Image.Resampling.LANCZOS)
        icon_sq.save(os.path.join(folder_path, "ic_launcher.png"))

        # 2. ic_launcher_round.png
        icon_rd = master_legacy_rd.resize((l_sz, l_sz), Image.Resampling.LANCZOS)
        icon_rd.save(os.path.join(folder_path, "ic_launcher_round.png"))

        # 3. ic_launcher_background.png
        bg_adapt = master_bg.resize((a_sz, a_sz), Image.Resampling.LANCZOS)
        bg_adapt.save(os.path.join(folder_path, "ic_launcher_background.png"))

        # 4. ic_launcher_foreground.png
        fg_adapt = master_fg.resize((a_sz, a_sz), Image.Resampling.LANCZOS)
        fg_adapt.save(os.path.join(folder_path, "ic_launcher_foreground.png"))

        print(f"Generated assets for {folder_name}: launcher={l_sz}px, adaptive={a_sz}px")

    # 6. Mipmap anydpi-v26 adaptive icon XML definitions
    anydpi_dir = os.path.join(res_dir, "mipmap-anydpi-v26")
    os.makedirs(anydpi_dir, exist_ok=True)

    xml_content = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
</adaptive-icon>
"""
    with open(os.path.join(anydpi_dir, "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(xml_content)

    with open(os.path.join(anydpi_dir, "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(xml_content)

    print("Generated adaptive icon XMLs in mipmap-anydpi-v26")

if __name__ == "__main__":
    generate_icons()
