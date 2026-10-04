import 'package:flutter/foundation.dart';

class AppConfig {
  /// Override with: flutter run --dart-define=API_BASE_URL=http://192.168.1.10:8080/api
  static const String _apiBaseUrlOverride = String.fromEnvironment('API_BASE_URL');

  static String get apiBaseUrl {
    if (_apiBaseUrlOverride.isNotEmpty) return _apiBaseUrlOverride;
    // The Android emulator reaches the host machine through 10.0.2.2.
    if (!kIsWeb && defaultTargetPlatform == TargetPlatform.android) {
      return 'http://10.0.2.2:8080/api';
    }
    return 'http://localhost:8080/api';
  }

  static const Duration requestTimeout = Duration(seconds: 15);
}
