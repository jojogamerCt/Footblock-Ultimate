import math
import os
from svglib.svglib import svg2rlg
from reportlab.graphics import renderPM

# Mod details
MOD_ID = "footblockultimate"
SCALE = 22.0
SCREEN_CX = 256.0
SCREEN_CY = 205.0
YAW_DEG = 45.0
PITCH_DEG = 28.0

def project(vx, vy, vz):
    # Center coordinates around the ball's center (2.5, 2.5, 2.5)
    cx = vx - 2.5
    cy = vy - 2.5
    cz = vz - 2.5
    
    # Yaw rotation (around Y axis)
    rad_yaw = math.radians(YAW_DEG)
    x1 = cx * math.cos(rad_yaw) - cz * math.sin(rad_yaw)
    y1 = cy
    z1 = cx * math.sin(rad_yaw) + cz * math.cos(rad_yaw)
    
    # Pitch rotation (around X axis)
    rad_pitch = math.radians(PITCH_DEG)
    x2 = x1
    y2 = y1 * math.cos(rad_pitch) - z1 * math.sin(rad_pitch)
    z2 = y1 * math.sin(rad_pitch) + z1 * math.cos(rad_pitch)
    
    # 2D screen coordinates
    u = x2 * SCALE + SCREEN_CX
    v = -y2 * SCALE + SCREEN_CY  # Invert Y for screen space
    
    return u, v, z2

def is_black(x, y, z):
    # Center of top/bottom caps (2x2)
    if y == 6 and 2 <= x <= 3 and 2 <= z <= 3:
        return True
    if y == -1 and 2 <= x <= 3 and 2 <= z <= 3:
        return True
    # Center of left/right caps (2x2)
    if x == 6 and 2 <= y <= 3 and 2 <= z <= 3:
        return True
    if x == -1 and 2 <= y <= 3 and 2 <= z <= 3:
        return True
    # Center of front/back caps (2x2)
    if z == 6 and 2 <= x <= 3 and 2 <= y <= 3:
        return True
    if z == -1 and 2 <= x <= 3 and 2 <= y <= 3:
        return True
    
    # 8 corners of the 6x6x6 center cube
    if x in (0, 5) and y in (0, 5) and z in (0, 5):
        return True
        
    return False

def generate_svg():
    # Build list of all voxels
    voxels = []
    
    # 6x6x6 center
    for x in range(6):
        for y in range(6):
            for z in range(6):
                voxels.append((x, y, z))
                
    # Caps (4x4)
    for x in range(1, 5):
        for z in range(1, 5):
            voxels.append((x, 6, z))   # Top
            voxels.append((x, -1, z))  # Bottom
            
    for y in range(1, 5):
        for z in range(1, 5):
            voxels.append((6, y, z))   # Right
            voxels.append((-1, y, z))  # Left
            
    for x in range(1, 5):
        for y in range(1, 5):
            voxels.append((x, y, 6))   # Front
            voxels.append((x, y, -1))  # Back
            
    # Calculate depth and sort voxels back-to-front
    voxel_depths = []
    for x, y, z in voxels:
        vc_x = x + 0.5
        vc_y = y + 0.5
        vc_z = z + 0.5
        _, _, depth = project(vc_x, vc_y, vc_z)
        voxel_depths.append((depth, (x, y, z)))
        
    voxel_depths.sort(key=lambda item: item[0])
    
    svg_polygons = []
    for depth, (x, y, z) in voxel_depths:
        black = is_black(x, y, z)
        
        # 1. Top face (+Y)
        pts_top = [
            project(x, y+1, z)[:2],
            project(x+1, y+1, z)[:2],
            project(x+1, y+1, z+1)[:2],
            project(x, y+1, z+1)[:2],
        ]
        color_top = "#3a3a3d" if black else "#ffffff"
        pts_str_top = " ".join(f"{u:.2f},{v:.2f}" for u, v in pts_top)
        svg_polygons.append(f'  <polygon points="{pts_str_top}" fill="{color_top}" stroke="#161618" stroke-width="0.5" stroke-linejoin="round" />')
        
        # 2. Right face (+X)
        pts_right = [
            project(x+1, y, z)[:2],
            project(x+1, y+1, z)[:2],
            project(x+1, y+1, z+1)[:2],
            project(x+1, y, z+1)[:2],
        ]
        color_right = "#222224" if black else "#dedede"
        pts_str_right = " ".join(f"{u:.2f},{v:.2f}" for u, v in pts_right)
        svg_polygons.append(f'  <polygon points="{pts_str_right}" fill="{color_right}" stroke="#161618" stroke-width="0.5" stroke-linejoin="round" />')
        
        # 3. Front face (+Z)
        pts_front = [
            project(x, y, z+1)[:2],
            project(x+1, y, z+1)[:2],
            project(x+1, y+1, z+1)[:2],
            project(x, y+1, z+1)[:2],
        ]
        color_front = "#141416" if black else "#bcbcbc"
        pts_str_front = " ".join(f"{u:.2f},{v:.2f}" for u, v in pts_front)
        svg_polygons.append(f'  <polygon points="{pts_str_front}" fill="{color_front}" stroke="#161618" stroke-width="0.5" stroke-linejoin="round" />')

    ball_elements_str = "\n".join(svg_polygons)
    
    # Template for the SVG
    svg_template = f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <defs>
    <!-- Background Shield Gradient -->
    <radialGradient id="bgGrad" cx="50%" cy="40%" r="60%">
      <stop offset="0%" stop-color="#1b4d2c" /> <!-- Rich dark green -->
      <stop offset="70%" stop-color="#0b1c11" />
      <stop offset="100%" stop-color="#030704" />
    </radialGradient>
    
    <!-- Golden Border Gradient -->
    <linearGradient id="goldGrad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#ffe066" />
      <stop offset="30%" stop-color="#f5a623" />
      <stop offset="70%" stop-color="#d07b0d" />
      <stop offset="100%" stop-color="#ffe066" />
    </linearGradient>

    <!-- Blur Filter for Shadow -->
    <filter id="shadowBlur" x="-30%" y="-30%" width="160%" height="160%">
      <feGaussianBlur stdDeviation="10" />
    </filter>
  </defs>

  <!-- Deep Premium Background Circle -->
  <circle cx="256" cy="256" r="236" fill="url(#bgGrad)" stroke="url(#goldGrad)" stroke-width="8" />

  <!-- Inner Golden Ring -->
  <circle cx="256" cy="256" r="218" fill="none" stroke="url(#goldGrad)" stroke-width="2.5" stroke-dasharray="10 8" opacity="0.5" />

  <!-- Ball Shadow -->
  <ellipse cx="256" cy="325" rx="100" ry="18" fill="#000000" opacity="0.65" filter="url(#shadowBlur)" />
  
  <!-- 3D Voxel Football Ball -->
  <g id="football">
{ball_elements_str}
  </g>
  
  <!-- Mod Title Text Elements -->
  <g id="titleText">
    <!-- Drop shadow/Backdrop for FOOTBLOCK -->
    <text x="256" y="418" text-anchor="middle" font-family="'Outfit', 'Inter', 'Montserrat', 'Arial Black', sans-serif" font-weight="900" font-size="44" fill="#020503" stroke="#020503" stroke-width="10" stroke-linejoin="round">FOOTBLOCK</text>
    <text x="256" y="418" text-anchor="middle" font-family="'Outfit', 'Inter', 'Montserrat', 'Arial Black', sans-serif" font-weight="900" font-size="44" fill="#ffffff">FOOTBLOCK</text>
    
    <!-- Sporty Ribbon / Trapezoid Banner for ULTIMATE -->
    <path d="M 120 432 L 392 432 L 372 466 L 140 466 Z" fill="url(#goldGrad)" />
    <!-- ULTIMATE Text -->
    <text x="256" y="457" text-anchor="middle" font-family="'Outfit', 'Inter', 'Montserrat', 'Arial', sans-serif" font-weight="900" font-size="22" fill="#0b1c11" letter-spacing="4">ULTIMATE</text>
  </g>
</svg>
'''
    return svg_template

def find_chrome():
    paths = [
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
        os.path.expandvars(r"%LocalAppData%\Google\Chrome\Application\chrome.exe"),
    ]
    for p in paths:
        if os.path.exists(p):
            return p
    return None

def main():
    print("Generating SVG mod icon...")
    svg_content = generate_svg()
    
    # Define directories
    target_res_dir = os.path.join("common", "src", "main", "resources", "assets", MOD_ID)
    os.makedirs(target_res_dir, exist_ok=True)
    
    svg_path = os.path.abspath(os.path.join(target_res_dir, "icon.svg"))
    png_path = os.path.abspath(os.path.join(target_res_dir, "icon.png"))
    cf_png_path = os.path.abspath("curseforge_icon.png")
    
    # Save SVG to mod resources
    with open(svg_path, "w", encoding="utf-8") as f:
        f.write(svg_content)
    print(f"Saved SVG icon to: {svg_path}")
    
    # Create HTML wrapper for perfect rendering
    html_content = f'''<!DOCTYPE html>
<html>
<head>
<style>
  html, body {{
    margin: 0;
    padding: 0;
    width: 512px;
    height: 512px;
    overflow: hidden;
    background: transparent;
  }}
  svg {{
    width: 512px;
    height: 512px;
    display: block;
  }}
</style>
</head>
<body>
  {svg_content}
</body>
</html>
'''
    html_path = os.path.abspath("temp_icon.html")
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html_content)
        
    chrome_path = find_chrome()
    if not chrome_path:
        print("Error: Google Chrome not found! Cannot render SVG to PNG.")
        print("Please render icon.svg to icon.png manually.")
        return
        
    print(f"Using Google Chrome at: {chrome_path}")
    import subprocess
    
    # Render using headless Chrome
    temp_screenshot = os.path.abspath("temp_screenshot.png")
    cmd = [
        chrome_path,
        "--headless",
        "--disable-gpu",
        "--default-background-color=00000000",
        f"--screenshot={temp_screenshot}",
        "--window-size=512,512",
        f"file:///{html_path.replace(os.sep, '/')}"
    ]
    
    print("Running headless Chrome...")
    try:
        subprocess.run(cmd, check=True)
        
        # Copy to destinations
        import shutil
        shutil.copyfile(temp_screenshot, png_path)
        shutil.copyfile(temp_screenshot, cf_png_path)
        print(f"Saved PNG icon to mod resources: {png_path}")
        print(f"Saved PNG icon to project root: {cf_png_path}")
        
    except Exception as e:
        print(f"Error rendering with Chrome: {e}")
    finally:
        # Clean up temp files
        if os.path.exists(html_path):
            os.remove(html_path)
        if os.path.exists(temp_screenshot):
            os.remove(temp_screenshot)
            
    print("Icon generation completed successfully!")

if __name__ == "__main__":
    main()
