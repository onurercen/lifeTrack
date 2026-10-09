import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';

import '../features/auth/presentation/login_screen.dart';
import '../features/auth/presentation/verify_email_screen.dart';
import '../features/auth/state/auth_controller.dart';
import '../features/home/presentation/home_shell.dart';
import '../features/splash/presentation/splash_screen.dart';
import 'app_scope.dart';
import 'routes.dart';

class LifeTrackApp extends StatefulWidget {
  const LifeTrackApp({super.key, required this.dependencies});

  final AppDependencies dependencies;

  @override
  State<LifeTrackApp> createState() => _LifeTrackAppState();
}

class _LifeTrackAppState extends State<LifeTrackApp> {
  final _navigatorKey = GlobalKey<NavigatorState>();
  late AuthStatus _lastStatus;

  AuthController get _auth => widget.dependencies.auth;

  @override
  void initState() {
    super.initState();
    _lastStatus = _auth.status;
    _auth.addListener(_onAuthChanged);
  }

  @override
  void dispose() {
    _auth.removeListener(_onAuthChanged);
    super.dispose();
  }

  // When the session starts or ends, drop any pushed routes (e.g. register)
  // so the root screen below reflects the new state.
  void _onAuthChanged() {
    if (_auth.status == _lastStatus) return;
    _lastStatus = _auth.status;
    _navigatorKey.currentState?.popUntil((route) => route.isFirst);
  }

  @override
  Widget build(BuildContext context) {
    return AppScope(
      dependencies: widget.dependencies,
      child: MaterialApp(
        title: 'LifeTrack',
        navigatorKey: _navigatorKey,
        debugShowCheckedModeBanner: false,
        routes: AppRoutes.routes,
        locale: const Locale('tr'),
        supportedLocales: const [Locale('tr')],
        localizationsDelegates: GlobalMaterialLocalizations.delegates,
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF0F766E)),
          useMaterial3: true,
        ),
        home: const _AuthGate(),
      ),
    );
  }
}

class _AuthGate extends StatelessWidget {
  const _AuthGate();

  @override
  Widget build(BuildContext context) {
    final auth = AppScope.of(context).auth;
    return switch (auth.status) {
      AuthStatus.unknown => const SplashScreen(),
      // A null user means the profile couldn't be loaded (offline); show the app.
      AuthStatus.authenticated when auth.user?.emailVerified == false => const VerifyEmailScreen(),
      AuthStatus.authenticated => const HomeShell(),
      AuthStatus.unauthenticated => const LoginScreen(),
    };
  }
}
