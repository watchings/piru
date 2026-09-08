import 'package:flutter/material.dart';

void main() {
  runApp(const PiruFlutterShell());
}

class PiruFlutterShell extends StatelessWidget {
  const PiruFlutterShell({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Piru',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xffeb4470)),
        useMaterial3: true,
      ),
      home: Scaffold(
        appBar: AppBar(title: const Text('Piru')),
        body: const Center(
          child: Text('Flutter migration build shell'),
        ),
      ),
    );
  }
}
