import 'package:flutter/widgets.dart';

import '../core/network/api_client.dart';
import '../core/storage/auth_storage.dart';
import '../core/utils/file_sharer.dart';
import '../features/auth/state/auth_controller.dart';

/// Shared app-wide dependencies, available via [AppScope.of].
class AppDependencies {
  AppDependencies({required this.api, required this.auth, this.shareFile = shareWithSystem});

  factory AppDependencies.create({ApiClient? api, AuthStorage? storage, FileSharer? shareFile}) {
    late final AuthController auth;
    final client = api ?? ApiClient(tokenProvider: () => auth.token);
    auth = AuthController(api: client, storage: storage ?? AuthStorage());
    client
      ..onUnauthorized = auth.handleUnauthorized
      ..refreshSession = auth.refreshSession;
    return AppDependencies(api: client, auth: auth, shareFile: shareFile ?? shareWithSystem);
  }

  final ApiClient api;
  final AuthController auth;
  final FileSharer shareFile;

  void dispose() {
    auth.dispose();
    api.dispose();
  }
}

class AppScope extends InheritedNotifier<AuthController> {
  AppScope({super.key, required this.dependencies, required super.child})
      : super(notifier: dependencies.auth);

  final AppDependencies dependencies;

  /// Rebuilds the caller when the auth state changes.
  static AppDependencies of(BuildContext context) {
    final scope = context.dependOnInheritedWidgetOfExactType<AppScope>();
    assert(scope != null, 'AppScope not found in widget tree');
    return scope!.dependencies;
  }

  /// Reads dependencies without subscribing to changes (e.g. in callbacks).
  static AppDependencies read(BuildContext context) {
    final scope = context.getInheritedWidgetOfExactType<AppScope>();
    assert(scope != null, 'AppScope not found in widget tree');
    return scope!.dependencies;
  }
}
