import 'package:flutter_secure_storage/test/test_flutter_secure_storage_platform.dart';
import 'package:flutter_secure_storage_platform_interface/flutter_secure_storage_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:lifetrack_mobile/core/storage/auth_storage.dart';

void main() {
  test('tokenlar yeni storage örneğinden okunabilir ve birlikte silinir', () async {
    final backingStore = <String, String>{};
    FlutterSecureStoragePlatform.instance = TestFlutterSecureStoragePlatform(backingStore);

    final firstStorage = AuthStorage();
    await firstStorage.saveTokens(token: 'test.jwt.token', refreshToken: 'refresh');

    final secondStorage = AuthStorage();
    expect(await secondStorage.readToken(), 'test.jwt.token');
    expect(await secondStorage.readRefreshToken(), 'refresh');

    await secondStorage.clear();
    expect(await firstStorage.readToken(), isNull);
    expect(await firstStorage.readRefreshToken(), isNull);
  });
}