async function subscribePush(){
    const permission = await Notification.requestPermission();
    if(permission !== 'granted') return;

    const registration = await navigator.serviceWorker.register('/sw.js');
    const subscription = await registration.pushManager.subscribe({
        userVisibleOnly:true,
        applicationServerKey:'BJNwvs_ytTbfi94zVcmvfrjNKIr3mGbWylR3xd-LnDmGQEJ7UD8qIOruTvyu6Dd4DpFWNbEJvzjg7sqT1buH_dc'
    });
    await fetch('subscribe',{
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify((subscription))
    })
}