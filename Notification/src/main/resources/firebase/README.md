Place the Firebase Admin SDK private key here with this exact filename:

service-account.json

Expected final path:

Notification/src/main/resources/firebase/service-account.json

To run the notification service with real Firebase push:

1. Download the private key from Firebase Console:
   Project settings > Service accounts > Generate new private key
2. Save it here as service-account.json
3. Start the service with:
   - mvn clean spring-boot:run

If you want to temporarily disable real Firebase and use the mock sender instead:

- set FCM_ENABLED=false before starting the service
