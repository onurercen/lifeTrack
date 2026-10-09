import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_exception.dart';
import '../../../core/storage/auth_storage.dart';
import '../models/auth_response.dart';

enum AuthStatus { unknown, authenticated, unauthenticated }

/// Single source of truth for the user's session.
class AuthController extends ChangeNotifier {
  AuthController({required ApiClient api, required AuthStorage storage})
      : _api = api,
        _storage = storage;

  final ApiClient _api;
  final AuthStorage _storage;

  AuthStatus _status = AuthStatus.unknown;
  String? _token;
  String? _refreshToken;
  AuthUser? _user;

  AuthStatus get status => _status;
  String? get token => _token;
  AuthUser? get user => _user;

  /// Restores a saved session on app start.
  Future<void> restore() async {
    final token = await _storage.readToken();
    if (token == null) {
      _set(AuthStatus.unauthenticated);
      return;
    }

    _token = token;
    // Missing for sessions saved before refresh tokens existed; those end when the token expires.
    _refreshToken = await _storage.readRefreshToken();
    try {
      _user = AuthUser.fromJson(await _api.get('users/me') as Map<String, dynamic>);
      _set(AuthStatus.authenticated);
    } on ApiException catch (e) {
      if (e.isUnauthorized) {
        await _clear();
      } else {
        // Server unreachable: keep the session, the profile is loaded later.
        _set(AuthStatus.authenticated);
      }
    }
  }

  Future<void> login({required String email, required String password}) async {
    final json = await _api.post(
      'auth/login',
      body: {'email': email, 'password': password},
      authenticated: false,
    );
    await _startSession(AuthResponse.fromJson(json as Map<String, dynamic>));
  }

  Future<void> register({
    required String name,
    required String email,
    required String password,
  }) async {
    final json = await _api.post(
      'auth/register',
      body: {'name': name, 'email': email, 'password': password},
      authenticated: false,
    );
    await _startSession(AuthResponse.fromJson(json as Map<String, dynamic>));
  }

  /// Confirms the e-mail address with the code sent at registration.
  Future<void> verifyEmail(String code) async {
    final json = await _api.post('users/me/verify-email', body: {'code': code});
    _user = AuthUser.fromJson(json as Map<String, dynamic>);
    notifyListeners();
  }

  /// Sends a new verification code; the server allows one per minute.
  Future<void> resendVerificationCode() => _api.post('users/me/verify-email/resend');

  /// E-mails a reset code if [email] belongs to an account; succeeds either way.
  Future<void> forgotPassword(String email) =>
      _api.post('auth/forgot-password', body: {'email': email}, authenticated: false);

  /// Sets a new password with the e-mailed code and signs in. Other devices are signed out.
  Future<void> resetPassword({required String email, required String code, required String newPassword}) async {
    final json = await _api.post(
      'auth/reset-password',
      body: {'email': email, 'code': code, 'newPassword': newPassword},
      authenticated: false,
    );
    await _startSession(AuthResponse.fromJson(json as Map<String, dynamic>));
  }

  /// Ends the session locally right away and tells the server in the background.
  Future<void> logout() async {
    final refreshToken = _refreshToken;
    if (refreshToken != null) {
      unawaited(
        _api
            .post('auth/logout', body: {'refreshToken': refreshToken}, authenticated: false)
            .then((_) {}, onError: (Object _) {}),
      );
    }
    await _clear();
  }

  Future<void> updateName(String name) async {
    final json = await _api.put('users/me', body: {'name': name});
    _user = AuthUser.fromJson(json as Map<String, dynamic>);
    notifyListeners();
  }

  /// Other devices are signed out; this one gets a new token pair.
  Future<void> changePassword({required String currentPassword, required String newPassword}) async {
    final json = await _api.put(
      'users/me/password',
      body: {'currentPassword': currentPassword, 'newPassword': newPassword},
    );
    await _saveSession(AuthResponse.fromJson(json as Map<String, dynamic>));
    notifyListeners();
  }

  /// Deletes the account and all its data on the server, then signs out.
  Future<void> deleteAccount({required String password}) async {
    await _api.delete('users/me', body: {'password': password});
    await _clear();
  }

  /// Called by [ApiClient] after a 401 to get a new token pair.
  Future<bool> refreshSession() async {
    final refreshToken = _refreshToken;
    if (refreshToken == null) return false;
    try {
      final json = await _api.post('auth/refresh', body: {'refreshToken': refreshToken}, authenticated: false);
      final response = AuthResponse.fromJson(json as Map<String, dynamic>);
      // The user may have logged out while the request was in flight.
      if (_refreshToken != refreshToken) return false;
      await _saveSession(response);
      notifyListeners();
      return true;
    } on ApiException catch (e) {
      if (e.isUnauthorized) return false;
      rethrow;
    }
  }

  /// Called by [ApiClient] when an authenticated request returns 401.
  void handleUnauthorized() {
    if (_status == AuthStatus.authenticated) _clear();
  }

  Future<void> _startSession(AuthResponse response) async {
    await _saveSession(response);
    _set(AuthStatus.authenticated);
  }

  Future<void> _saveSession(AuthResponse response) async {
    _token = response.token;
    _refreshToken = response.refreshToken;
    _user = response.user;
    await _storage.saveTokens(token: response.token, refreshToken: response.refreshToken);
  }

  Future<void> _clear() async {
    _token = null;
    _refreshToken = null;
    _user = null;
    await _storage.clear();
    _set(AuthStatus.unauthenticated);
  }

  void _set(AuthStatus status) {
    _status = status;
    notifyListeners();
  }
}
