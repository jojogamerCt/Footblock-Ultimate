import os
import zlib
import struct
import json

def write_png(buf, width, height, filepath):
    # Ensure directory exists
    os.makedirs(os.path.dirname(filepath), exist_ok=True)
    
    scanlines = []
    for y in range(height):
        scanlines.append(b'\x00') # Filter type 0
        row_data = buf[y * width * 4 : (y + 1) * width * 4]
        scanlines.append(row_data)
    flat_data = b''.join(scanlines)
    compressed = zlib.compress(flat_data)
    
    png = bytearray(b'\x89PNG\r\n\x1a\n')
    
    # IHDR chunk
    ihdr_data = struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)
    png.extend(struct.pack('>I', 13))
    png.extend(b'IHDR')
    png.extend(ihdr_data)
    png.extend(struct.pack('>I', zlib.crc32(b'IHDR' + ihdr_data)))
    
    # IDAT chunk
    png.extend(struct.pack('>I', len(compressed)))
    png.extend(b'IDAT')
    png.extend(compressed)
    png.extend(struct.pack('>I', zlib.crc32(b'IDAT' + compressed)))
    
    # IEND chunk
    png.extend(struct.pack('>I', 0))
    png.extend(b'IEND')
    png.extend(struct.pack('>I', zlib.crc32(b'IEND')))
    
    with open(filepath, 'wb') as f:
        f.write(png)

# 64x64 RGBA buffer
width, height = 64, 64
buf = bytearray([0, 0, 0, 0] * width * height)

def set_pixel(x, y, r, g, b, a):
    idx = (y * width + x) * 4
    buf[idx] = r
    buf[idx+1] = g
    buf[idx+2] = b
    buf[idx+3] = a

def fill_rect(u, v, w, h, color):
    for y in range(h):
        for x in range(w):
            px = u + x
            py = v + y
            if 0 <= px < 64 and 0 <= py < 64:
                # Add subtle noise/shading
                noise = (px * 31 + py * 17) % 16 - 8
                r = max(0, min(255, color[0] + noise))
                g = max(0, min(255, color[1] + noise))
                b = max(0, min(255, color[2] + noise))
                set_pixel(px, py, r, g, b, 255)

def paint_box(u, v, x_size, y_size, z_size):
    # West face (size Z x Y)
    fill_rect(u, v + z_size, z_size, y_size, (240, 240, 240))
    # North face (size X x Y)
    fill_rect(u + z_size, v + z_size, x_size, y_size, (240, 240, 240))
    # East face (size Z x Y)
    fill_rect(u + z_size + x_size, v + z_size, z_size, y_size, (240, 240, 240))
    # South face (size X x Y)
    fill_rect(u + 2 * z_size + x_size, v + z_size, x_size, y_size, (240, 240, 240))
    # Up face (size X x Z)
    fill_rect(u + z_size, v, x_size, z_size, (240, 240, 240))
    # Down face (size X x Z)
    fill_rect(u + z_size + x_size, v, x_size, z_size, (240, 240, 240))

# 1. Paint center box (size 6x6x6 at U=0, V=0)
paint_box(0, 0, 6, 6, 6)

# Corners of 6x6 faces of center box are dark spots
# We will draw a dark 1x1 spot at each corner of each face to simulate soccer patterns
center_faces = [
    (6, 0),   # Up
    (12, 0),  # Down
    (0, 6),   # West
    (6, 6),   # North
    (12, 6),  # East
    (18, 6),  # South
]
for u, v in center_faces:
    # Corner pixels in the face
    for x, y in [(0, 0), (5, 0), (0, 5), (5, 5)]:
        fill_rect(u + x, v + y, 1, 1, (40, 40, 40))

# 2. Paint caps (each cap is size 4x4x1 or 1x4x4 or 4x1x4)
# Top Cap (4, 1, 4) at (0, 20)
paint_box(0, 20, 4, 1, 4)
# Main face: Up face is at [4, 20] size 4x4. Let's paint center 2x2 dark.
fill_rect(4 + 1, 20 + 1, 2, 2, (40, 40, 40))

# Bottom Cap (4, 1, 4) at (0, 25)
paint_box(0, 25, 4, 1, 4)
# Main face: Down face is at [8, 25] size 4x4. Let's paint center 2x2 dark.
fill_rect(8 + 1, 25 + 1, 2, 2, (40, 40, 40))

# North Cap (4, 4, 1) at (16, 20)
paint_box(16, 20, 4, 4, 1)
# Main face: North face is at [17, 21] size 4x4. Let's paint center 2x2 dark.
fill_rect(17 + 1, 21 + 1, 2, 2, (40, 40, 40))

# South Cap (4, 4, 1) at (16, 25)
paint_box(16, 25, 4, 4, 1)
# Main face: South face is at [22, 26] size 4x4. Let's paint center 2x2 dark.
fill_rect(22 + 1, 26 + 1, 2, 2, (40, 40, 40))

# West Cap (1, 4, 4) at (32, 20)
paint_box(32, 20, 1, 4, 4)
# Main face: West face is at [32, 24] size 4x4. Let's paint center 2x2 dark.
fill_rect(32 + 1, 24 + 1, 2, 2, (40, 40, 40))

# East Cap (1, 4, 4) at (32, 25)
paint_box(32, 25, 1, 4, 4)
# Main face: East face is at [37, 29] size 4x4. Let's paint center 2x2 dark.
fill_rect(37 + 1, 29 + 1, 2, 2, (40, 40, 40))

# Write out texture files
root_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
tex_item_path = os.path.join(root_dir, 'common', 'src', 'main', 'resources', 'assets', 'footblockultimate', 'textures', 'item', 'football.png')
tex_entity_path = os.path.join(root_dir, 'common', 'src', 'main', 'resources', 'assets', 'footblockultimate', 'textures', 'entity', 'football.png')

write_png(buf, 64, 64, tex_item_path)
write_png(buf, 64, 64, tex_entity_path)

print(f"Generated textures at:\n  {tex_item_path}\n  {tex_entity_path}")

# Write out JSON model file
model_json_path = os.path.join(root_dir, 'common', 'src', 'main', 'resources', 'assets', 'footblockultimate', 'models', 'item', 'football.json')

# Helper to format JSON UV from pixel UV (scaled down by /4 for Minecraft's [0, 16] space)
def make_faces(u, v, x_size, y_size, z_size):
    return {
        "down":  {"uv": [(u + z_size + x_size) / 4.0, (v) / 4.0, (u + 2 * z_size + x_size) / 4.0, (v + z_size) / 4.0], "texture": "#ball"},
        "up":    {"uv": [(u + z_size) / 4.0, (v) / 4.0, (u + z_size + x_size) / 4.0, (v + z_size) / 4.0], "texture": "#ball"},
        "north": {"uv": [(u + z_size) / 4.0, (v + z_size) / 4.0, (u + z_size + x_size) / 4.0, (v + z_size + y_size) / 4.0], "texture": "#ball"},
        "south": {"uv": [(u + 2 * z_size + x_size) / 4.0, (v + z_size) / 4.0, (u + 2 * z_size + 2 * x_size) / 4.0, (v + z_size + y_size) / 4.0], "texture": "#ball"},
        "west":  {"uv": [(u) / 4.0, (v + z_size) / 4.0, (u + z_size) / 4.0, (v + z_size + y_size) / 4.0], "texture": "#ball"},
        "east":  {"uv": [(u + z_size + x_size) / 4.0, (v + z_size) / 4.0, (u + 2 * z_size + x_size) / 4.0, (v + z_size + y_size) / 4.0], "texture": "#ball"}
    }

model_data = {
    "texture_size": [64, 64],
    "textures": {
        "ball": "footblockultimate:item/football"
    },
    "elements": [
        # Center box (5, 5, 5) to (11, 11, 11)
        {
            "from": [5, 5, 5],
            "to": [11, 11, 11],
            "faces": make_faces(0, 0, 6, 6, 6)
        },
        # Top Cap (6, 11, 6) to (10, 12, 10)
        {
            "from": [6, 11, 6],
            "to": [10, 12, 10],
            "faces": make_faces(0, 20, 4, 1, 4)
        },
        # Bottom Cap (6, 4, 6) to (10, 5, 10)
        {
            "from": [6, 4, 6],
            "to": [10, 5, 10],
            "faces": make_faces(0, 25, 4, 1, 4)
        },
        # North Cap (6, 6, 4) to (10, 10, 5)
        {
            "from": [6, 6, 4],
            "to": [10, 10, 5],
            "faces": make_faces(16, 20, 4, 4, 1)
        },
        # South Cap (6, 6, 11) to (10, 10, 12)
        {
            "from": [6, 6, 11],
            "to": [10, 10, 12],
            "faces": make_faces(16, 25, 4, 4, 1)
        },
        # West Cap (4, 6, 6) to (5, 10, 10)
        {
            "from": [4, 6, 6],
            "to": [5, 10, 10],
            "faces": make_faces(32, 20, 1, 4, 4)
        },
        # East Cap (11, 6, 6) to (12, 10, 10)
        {
            "from": [11, 6, 6],
            "to": [12, 10, 10],
            "faces": make_faces(32, 25, 1, 4, 4)
        }
    ],
    "display": {
        "thirdperson_righthand": {
            "rotation": [0, 0, 0],
            "translation": [0, 3, 1],
            "scale": [0.85, 0.85, 0.85]
        },
        "firstperson_righthand": {
            "rotation": [0, 0, 0],
            "translation": [1.13, 3.2, 1.13],
            "scale": [0.75, 0.75, 0.75]
        },
        "ground": {
            "translation": [0, 2, 0],
            "scale": [0.8, 0.8, 0.8]
        },
        "gui": {
            "rotation": [30, 45, 0],
            "translation": [0, 0, 0],
            "scale": [0.85, 0.85, 0.85]
        },
        "fixed": {
            "scale": [0.8, 0.8, 0.8]
        }
    }
}

os.makedirs(os.path.dirname(model_json_path), exist_ok=True)
with open(model_json_path, 'w') as f:
    json.dump(model_data, f, indent=2)

print(f"Generated item model at:\n  {model_json_path}")
