import sharp from 'sharp';
import {readFile,mkdir} from 'node:fs/promises';
const icon=await readFile(new URL('../src/app/icon.svg',import.meta.url));
const directory=new URL('../public/icons/',import.meta.url);await mkdir(directory,{recursive:true});
for(const size of [192,512])await sharp(icon).resize(size,size).png().toFile(new URL(`icon-${size}.png`,directory).pathname.replace(/^\/(\w:)/,'$1'));
await sharp(icon).resize(180,180).png().toFile(new URL('apple-touch-icon.png',directory).pathname.replace(/^\/(\w:)/,'$1'));
const foreground=await sharp(icon).resize(320,320).png().toBuffer();await sharp({create:{width:512,height:512,channels:4,background:'#6756A7'}}).composite([{input:foreground,gravity:'centre'}]).png().toFile(new URL('maskable-512.png',directory).pathname.replace(/^\/(\w:)/,'$1'));
