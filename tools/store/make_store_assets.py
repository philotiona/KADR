"""Store listing graphics for KADR (Lab 8, task 8).

Draws the 512x512 hi-res icon from the same geometry as res/drawable/ic_learning.xml
and a 1024x500 feature graphic. Output: distribution/store/.
Usage: python tools/store/make_store_assets.py
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'distribution' / 'store'
OUT.mkdir(parents=True, exist_ok=True)
YELLOW, INK, PAPER, ACCENT = '#F5CC55', '#191A1B', '#F5F1E8', '#FF7447'
FONT = 'C:/Windows/Fonts/arialbd.ttf'
FONT_REG = 'C:/Windows/Fonts/arial.ttf'


def icon(size):
    k = size / 108
    im = Image.new('RGB', (size, size), YELLOW)
    d = ImageDraw.Draw(im)
    box = lambda x, y, w, h, c: d.rectangle([x * k, y * k, (x + w) * k - 1, (y + h) * k - 1], fill=c)
    box(22, 24, 64, 60, INK)
    for y in (30, 43, 56, 69):
        box(28, y, 7, 7, PAPER)
        box(73, y, 7, 7, PAPER)
    d.polygon([(44 * k, 35 * k), (68 * k, 54 * k), (44 * k, 73 * k)], fill=ACCENT)
    return im


icon(512).save(OUT / 'icon-512.png')

fg = Image.new('RGB', (1024, 500), INK)
d = ImageDraw.Draw(fg)
for x in range(0, 1024, 64):                       # film perforation strip
    d.rectangle([x + 12, 18, x + 40, 38], fill='#2B2C2E')
    d.rectangle([x + 12, 462, x + 40, 482], fill='#2B2C2E')
fg.paste(icon(300), (660, 100))
d.text((64, 110), 'KADR.', font=ImageFont.truetype(FONT, 120), fill=YELLOW)
d.text((68, 260), 'Кино рядом. Место — ваше.', font=ImageFont.truetype(FONT, 40), fill=PAPER)
d.text((68, 320), 'Афиша · бронирование мест · QR-билет', font=ImageFont.truetype(FONT_REG, 30), fill='#B9B5AA')
fg.save(OUT / 'feature-graphic-1024x500.png')
print('saved', [p.name for p in OUT.iterdir()])
