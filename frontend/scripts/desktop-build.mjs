import {spawnSync} from 'node:child_process';
const value=process.env.MEETGRID_DESKTOP_URL;
if(!value)throw new Error('Set MEETGRID_DESKTOP_URL to the hosted HTTPS frontend origin.');
const url=new URL(value);if(url.protocol!=='https:'||url.username||url.password||url.pathname!=='/'||url.search||url.hash||url.host.includes('REPLACE'))throw new Error('Use a real HTTPS origin without paths, credentials, query strings or fragments.');
const config=JSON.stringify({app:{windows:[{label:'main',title:'MeetGrid',url:url.origin,width:1280,height:840,minWidth:360,minHeight:600}]}});
const args=['build','--config',config];if(process.argv.includes('--store'))args.push('--config','src-tauri/tauri.microsoft-store.conf.json');
// A Node entry point avoids shell interpolation of the configured URL.
const result=spawnSync(process.execPath,['node_modules/@tauri-apps/cli/tauri.js',...args],{stdio:'inherit'});process.exit(result.status??1);
