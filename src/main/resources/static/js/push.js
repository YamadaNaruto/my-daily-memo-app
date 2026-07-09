async function subscribePush(){
    const permission = await Notification.requestPermission();
    if(permission !== 'granted') return;

    const registration = await navigator.serviceWorker.register('/sw.js');
    const key = await fetch("/vapidPublicKey").then(r => r.text());
    const subscription = await registration.pushManager.subscribe({
        userVisibleOnly:true,
        applicationServerKey:urlBase64ToUint8Array(key)
    });
    await fetch('subscribe',{
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify((subscription))
    })
}
function urlBase64ToUint8Array(base64String){
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const raw = atob(base64);                       // base64 → 生バイト文字列
    const output = new Uint8Array(raw.length);
    for (let i = 0; i < raw.length; i++) output[i] = raw.charCodeAt(i);
    return output; 
}