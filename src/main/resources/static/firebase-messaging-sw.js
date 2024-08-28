// firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/8.6.1/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/8.6.1/firebase-messaging.js');

// Initialize Firebase
const firebaseConfig = {
    apiKey: "AIzaSyBU5ETXhHKgFQ1WdT8rOoxfj0AG0KeJSzw",
    authDomain: "commitbody-6773d.firebaseapp.com",
    projectId: "commitbody-6773d",
    storageBucket: "commitbody-6773d.appspot.com",
    messagingSenderId: "261021331874",
    appId: "1:261021331874:web:ac7f61b5f77eec803f02fb",
    measurementId: "G-DKN1QLNG18"
};
firebase.initializeApp(firebaseConfig);

const messaging = firebase.messaging();

messaging.onBackgroundMessage((payload) => {
    const title = 'Hello World';
    const options = {
        body: payload.data.status,
    };
    self.registration.showNotification(title, options);
});

// 알림 클릭 시 이벤트 처리
// 알림 클릭 시 이벤트 처리
self.addEventListener('notificationclick', function(event) {
    console.log("실행됨?")
    const click_action = event.notification.data.click_action; // 서버에서 전달된 click_action URL
    event.notification.close(); // 알림 닫기

    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(windowClients => {
            for (let client of windowClients) {
                // 이미 열린 창이 있다면 focus
                if (client.url === click_action && 'focus' in client) {
                    return client.focus();
                }
            }
            // 없으면 새 창 열기
            if (clients.openWindow) {
                return clients.openWindow(click_action);
            }
        })
    );
});
