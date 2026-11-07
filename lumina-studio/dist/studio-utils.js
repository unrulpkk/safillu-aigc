/* Browser-local helpers, also exported for the packaging checks. */
(function(root){
  const category=r=>r.mode==='document'?'document':['music','audio'].includes(r.mode)?'audio':['video','avatar','motion'].includes(r.mode)?'video':'image';
  function filterAssets(records,options={}){
    return records.filter(r=>!r.deleted).filter(r=>(!options.type||options.type==='all'||options.type==='liked'||category(r)===options.type)&&(options.type!=='liked'||r.liked))
      .filter(r=>!options.liked||r.liked).filter(r=>!options.upscaled||r.upscaled)
      .filter(r=>!options.resolutions?.length||options.resolutions.includes(r.resolution||'2K'))
      .filter(r=>!options.ratios?.length||options.ratios.includes(r.ratio||'16:9'))
      .filter(r=>(r.prompt||'').toLowerCase().includes((options.search||'').toLowerCase()))
      .sort((a,b)=>options.sort==='oldest'?a.created-b.created:b.created-a.created);
  }
  const crcTable=Array.from({length:256},(_,n)=>{for(let i=0;i<8;i++)n=n&1?0xedb88320^(n>>>1):n>>>1;return n>>>0;});
  function crc32(bytes){let crc=0xffffffff;for(const byte of bytes)crc=crcTable[(crc^byte)&255]^(crc>>>8);return (crc^0xffffffff)>>>0;}
  function header(size,values){const data=new Uint8Array(size),view=new DataView(data.buffer);for(const [offset,value,bits=32] of values){if(bits===16)view.setUint16(offset,value,true);else view.setUint32(offset,value,true);}return data;}
  async function zipStore(files){
    const chunks=[],central=[];let offset=0,centralLength=0;
    for(const file of files){const name=new TextEncoder().encode(file.name),bytes=new Uint8Array(await file.blob.arrayBuffer()),crc=crc32(bytes);
      const local=header(30,[[0,0x04034b50],[4,20,16],[6,0x800,16],[14,crc],[18,bytes.length],[22,bytes.length],[26,name.length,16]]);
      chunks.push(local,name,bytes);
      const dir=header(46,[[0,0x02014b50],[4,20,16],[6,20,16],[8,0x800,16],[16,crc],[20,bytes.length],[24,bytes.length],[28,name.length,16],[42,offset]]);
      central.push(dir,name);centralLength+=46+name.length;offset+=30+name.length+bytes.length;
    }
    return new Blob([...chunks,...central,header(22,[[0,0x06054b50],[8,files.length,16],[10,files.length,16],[12,centralLength],[16,offset]])],{type:'application/zip'});
  }
  const api={category,filterAssets,crc32,zipStore};root.StudioUtils=api;if(typeof module!=='undefined')module.exports=api;
})(typeof window!=='undefined'?window:globalThis);
