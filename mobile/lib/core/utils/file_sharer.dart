import 'dart:ui';

import 'package:share_plus/share_plus.dart';

/// Hands a file to the user: the share sheet on phones (save to Files, Drive,
/// mail...), a download in browsers without Web Share. [origin] anchors the
/// iPad popover. [name] is passed separately: an in-memory [XFile] has no name
/// on mobile platforms.
typedef FileSharer = Future<void> Function(XFile file, String name, {Rect? origin});

Future<void> shareWithSystem(XFile file, String name, {Rect? origin}) async {
  await SharePlus.instance.share(ShareParams(
    files: [file],
    fileNameOverrides: [name],
    sharePositionOrigin: origin,
  ));
}
