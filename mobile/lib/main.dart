import 'package:flutter/material.dart';

import 'app/app.dart';
import 'app/app_scope.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  final dependencies = AppDependencies.create();
  dependencies.auth.restore();
  runApp(LifeTrackApp(dependencies: dependencies));
}
