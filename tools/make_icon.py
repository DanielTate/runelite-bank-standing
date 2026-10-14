#!/usr/bin/env python3
# Draws the 25x25 Bank Standing skill icon from the pixel grid below.
#   python3 tools/make_icon.py src/main/resources/nz/tate/bankstanding/icon.png [icon.png]
# The optional second path gets the 48x48 Plugin Hub icon.
# Edit the grid or palette, then re-run. Needs Pillow.
import sys
from PIL import Image
# 25x25 bank booth front. . = transparent
G = """
.........................
..kkkkkkkkkkkkkkkkkkkkk..
..kRRRRRRRRRRRRRRRRRRRk..
.kRRrrrrrrrrrrrrrrrrrRRk.
.kkkkkkkkkkkkkkkkkkkkkkk.
..kWwwwwwwwwwwwwwwwwwWk..
..kWkkkkkkkkkkkkkkkkkWk..
..kWkbbkGbbkGbbkGbbkkWk..
..kWkbbkGbbkGbbkGbbkkWk..
..kWkbbkGbbkGbbkGbbkkWk..
..kWkbbkGbbkGbbkGbbkkWk..
..kWkbbkGbbkGbbkGbbkkWk..
..kWkkkkkkkkkkkkkkkkkWk..
.kkkkkkkkkkkkkkkkkkkkkkk.
.kLLLLLLLLLLLLLLLLLLLLLk.
.klllllllllllllllllllllk.
.kkkkkkkkkkkkkkkkkkkkkkk.
..kWwwwwwwwwwwwwwwwwwWk..
..kWwwdwwwwwwwwwwwdwwWk..
..kWwwdwwwwYYwwwwwdwwWk..
..kWwwdwwwYyyYwwwwdwwWk..
..kWwwdwwwwYYwwwwwdwwWk..
..kWwwdwwwwwwwwwwwdwwWk..
..kkkkkkkkkkkkkkkkkkkkk..
.........................
"""
P = {'k':(32,20,10),'R':(122,38,28),'r':(160,56,40),'W':(92,58,30),'w':(134,88,46),
     'b':(40,40,48),'G':(200,170,60),'L':(176,124,66),'l':(140,96,50),'d':(98,62,32),
     'Y':(214,176,48),'y':(250,220,90)}
rows=[r for r in G.strip('\n').split('\n')]
im=Image.new('RGBA',(25,25),(0,0,0,0))
for y,row in enumerate(rows):
    for x,c in enumerate(row[:25]):
        if c in P: im.putpixel((x,y),P[c]+(255,))
im.save(sys.argv[1])
if len(sys.argv) > 2:
    # Plugin Hub icon: the drawn area at 2x, centred on 48x48.
    art = im.crop(im.getchannel('A').getbbox())
    art = art.resize((art.width * 2, art.height * 2), Image.NEAREST)
    hub = Image.new('RGBA', (48, 48), (0, 0, 0, 0))
    hub.paste(art, ((48 - art.width) // 2, (48 - art.height) // 2))
    hub.save(sys.argv[2])

