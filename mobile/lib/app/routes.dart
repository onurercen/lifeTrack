import 'package:flutter/material.dart';
import '../features/auth/presentation/register_screen.dart';

/// Root screens (splash / login / home) are chosen by the auth state in
/// `app.dart`; only screens pushed on top of them are listed here.
class AppRoutes {
  static const String register = '/register';

  static Map<String, WidgetBuilder> get routes => {
        register: (context) => const RegisterScreen(),
      };
}
