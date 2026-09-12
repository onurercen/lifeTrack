import 'package:flutter_secure_storage/test/test_flutter_secure_storage_platform.dart';
import 'package:flutter_secure_storage_platform_interface/flutter_secure_storage_platform_interface.dart';
import 'package:flutter_test/flutter_test.dart';

import '../../../lib/core/storage/auth_storage.dart';

void main() {
  test('JWT yeni storage örneğinden okunabilir ve silinebilir', () async {
    final backingStore = <String, String>{};
    FlutterSecureStoragePlatform.instance = TestFlutterSecureStoragePlatform(backingStore);

    final firstStorage = AuthStorage();
    await firstStorage.saveToken('test.jwt.token');

    final secondStorage = AuthStorage();
    expect(await secondStorage.readToken(), 'test.jwt.token');

    await secondStorage.clearToken();
    expect(await firstStorage.readToken(), isNull);
  });
}