# CoronaTrackingApp

This app constantly scans for Bluetooth Low Energy Advertisements with the app's UUID. When it detects such a beacon it will connect and exchange
the encrypted timestamp/user-key pair as well as the timestamp as clear text. An infected user can upload the user-key to the server (server code [CoronaTrackingApp/app/Server_App](./CoronaTrackingApp/app/Server_App) . The app downloads these keys
periodically and runs the encryption algorithm for each of the keys and its recorded timestamps. If the result matches one of the previously
received, encrypted pairs, it will be considered in a periodic risk evaluation.
