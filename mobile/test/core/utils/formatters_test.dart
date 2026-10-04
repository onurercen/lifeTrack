import 'package:flutter_test/flutter_test.dart';
import 'package:lifetrack_mobile/core/utils/formatters.dart';

void main() {
  test('formatDecimal virgül kullanır ve gereksiz sıfırları atar', () {
    expect(formatDecimal(5.2), '5,2');
    expect(formatDecimal(10.0), '10');
    expect(formatDecimal(5.25, fractionDigits: 2), '5,25');
  });

  test('formatDuration saat ve dakikayı ayırır', () {
    expect(formatDuration(30), '30 dk');
    expect(formatDuration(60), '1 sa');
    expect(formatDuration(95), '1 sa 35 dk');
  });

  test('formatPace dakika:saniye /km döner', () {
    expect(formatPace(5.75), '5:45 /km');
    expect(formatPace(6), '6:00 /km');
  });
}
