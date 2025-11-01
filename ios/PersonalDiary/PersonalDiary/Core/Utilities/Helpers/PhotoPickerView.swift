//
//  PhotoPickerView.swift
//  PersonalDiary
//
//  Created by Claude Code on 2025-10-31.
//  Copyright © 2025 Personal Diary Platform. All rights reserved.
//

import SwiftUI
import PhotosUI

struct PhotoPickerView: UIViewControllerRepresentable {
    let onImageSelected: (UIImage) -> Void

    func makeUIViewController(context: Context) -> PHPickerViewController {
        var configuration = PHPickerConfiguration()
        configuration.filter = .images
        configuration.selectionLimit = 1

        let picker = PHPickerViewController(configuration: configuration)
        picker.delegate = context.coordinator
        return picker
    }

    func updateUIViewController(_ uiViewController: PHPickerViewController, context: Context) {}

    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }

    class Coordinator: NSObject, PHPickerViewControllerDelegate {
        let parent: PhotoPickerView

        init(_ parent: PhotoPickerView) {
            self.parent = parent
        }

        func picker(_ picker: PHPickerViewController, didFinishPicking results: [PHPickerResult]) {
            picker.dismiss(animated: true)

            guard let result = results.first else { return }

            result.itemProvider.loadObject(ofClass: UIImage.self) { object, error in
                if let image = object as? UIImage {
                    DispatchQueue.main.async {
                        self.parent.onImageSelected(image)
                    }
                }
            }
        }
    }
}
