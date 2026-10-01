import sharp from 'sharp';
import {readFile,mkdir,writeFile} from 'node:fs/promises';
const icon=await readFile(new URL('../src/app/icon.svg',import.meta.url));
const directory=new URL('../public/icons/',import.meta.url);await mkdir(directory,{recursive:true});
// Browser tabs need small raster icons as well as the scalable app mark.
const faviconSizes=[16,32,48];
const faviconImages=await Promise.all(faviconSizes.map(size=>sharp(icon).resize(size,size).png().toBuffer()));
await writeFile(new URL('favicon-32.png',directory),faviconImages[1]);
const icoHeader=Buffer.alloc(6+16*faviconSizes.length);
icoHeader.writeUInt16LE(1,2);
icoHeader.writeUInt16LE(faviconSizes.length,4);
let imageOffset=icoHeader.length;
faviconImages.forEach((image,index)=>{
 const entry=6+16*index;
 icoHeader[entry]=faviconSizes[index];icoHeader[entry+1]=faviconSizes[index];
 icoHeader.writeUInt16LE(1,entry+4);icoHeader.writeUInt16LE(32,entry+6);
 icoHeader.writeUInt32LE(image.length,entry+8);icoHeader.writeUInt32LE(imageOffset,entry+12);
 imageOffset+=image.length;
});
await writeFile(new URL('../src/app/favicon.ico',import.meta.url),Buffer.concat([icoHeader,...faviconImages]));
for(const size of [192,512])await sharp(icon).resize(size,size).png().toFile(new URL(`icon-${size}.png`,directory).pathname.replace(/^\/(\w:)/,'$1'));
await sharp(icon).resize(180,180).png().toFile(new URL('apple-touch-icon.png',directory).pathname.replace(/^\/(\w:)/,'$1'));
const foreground=await sharp(icon).resize(320,320).png().toBuffer();await sharp({create:{width:512,height:512,channels:4,background:'#6756A7'}}).composite([{input:foreground,gravity:'centre'}]).png().toFile(new URL('maskable-512.png',directory).pathname.replace(/^\/(\w:)/,'$1'));
