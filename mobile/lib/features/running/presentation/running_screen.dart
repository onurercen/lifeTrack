import 'package:flutter/material.dart';

class RunningScreen extends StatelessWidget {
  const RunningScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Koşu')),
      body: const Center(child: Text('Koşu ekranı')),
    );
  }
}
