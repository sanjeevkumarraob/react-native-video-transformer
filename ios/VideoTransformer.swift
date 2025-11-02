import Foundation
import AVFoundation
import React

@objc(VideoTransformer)
class VideoTransformer: NSObject {

  @objc
  func rotateVideo(_ inputPath: String, angle: Int, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {

    guard let inputURL = URL(string: inputPath.hasPrefix("file://") ? inputPath : "file://\(inputPath)") else {
      rejecter("INVALID_PATH", "Invalid input path", nil)
      return
    }

    let asset = AVAsset(url: inputURL)

    guard let videoTrack = asset.tracks(withMediaType: .video).first else {
      rejecter("NO_VIDEO_TRACK", "No video track found", nil)
      return
    }

    // Create composition
    let composition = AVMutableComposition()
    guard let compositionVideoTrack = composition.addMutableTrack(withMediaType: .video, preferredTrackID: kCMPersistentTrackID_Invalid) else {
      rejecter("COMPOSITION_ERROR", "Failed to create composition track", nil)
      return
    }

    do {
      try compositionVideoTrack.insertTimeRange(CMTimeRange(start: .zero, duration: asset.duration), of: videoTrack, at: .zero)
    } catch {
      rejecter("INSERT_ERROR", "Failed to insert time range: \(error.localizedDescription)", nil)
      return
    }

    // Add audio track if exists
    if let audioTrack = asset.tracks(withMediaType: .audio).first {
      if let compositionAudioTrack = composition.addMutableTrack(withMediaType: .audio, preferredTrackID: kCMPersistentTrackID_Invalid) {
        try? compositionAudioTrack.insertTimeRange(CMTimeRange(start: .zero, duration: asset.duration), of: audioTrack, at: .zero)
      }
    }

    // Get natural size
    let naturalSize = videoTrack.naturalSize
    let videoWidth = naturalSize.width
    let videoHeight = naturalSize.height

    // Create transform based on rotation angle
    var transform: CGAffineTransform
    var renderSize: CGSize

    if angle == 90 {
      // Rotate 90 degrees clockwise
      // First translate, then rotate
      transform = CGAffineTransform(translationX: videoHeight, y: 0)
      transform = transform.rotated(by: .pi / 2)
      renderSize = CGSize(width: videoHeight, height: videoWidth)
    } else if angle == -90 || angle == 270 {
      // Rotate 90 degrees counter-clockwise
      transform = CGAffineTransform(translationX: 0, y: videoWidth)
      transform = transform.rotated(by: -.pi / 2)
      renderSize = CGSize(width: videoHeight, height: videoWidth)
    } else if angle == 180 {
      // Rotate 180 degrees
      transform = CGAffineTransform(translationX: videoWidth, y: videoHeight)
      transform = transform.rotated(by: .pi)
      renderSize = CGSize(width: videoWidth, height: videoHeight)
    } else {
      // No rotation
      transform = .identity
      renderSize = CGSize(width: videoWidth, height: videoHeight)
    }

    // Create video composition with transform
    let videoComposition = AVMutableVideoComposition()
    let instruction = AVMutableVideoCompositionInstruction()
    instruction.timeRange = CMTimeRange(start: .zero, duration: asset.duration)

    let layerInstruction = AVMutableVideoCompositionLayerInstruction(assetTrack: compositionVideoTrack)

    // Note: We set the new rotation transform, ignoring any existing preferredTransform
    // because we want to apply a fresh rotation to the video as it is
    layerInstruction.setTransform(transform, at: .zero)

    instruction.layerInstructions = [layerInstruction]
    videoComposition.instructions = [instruction]

    videoComposition.renderSize = renderSize
    videoComposition.frameDuration = CMTime(value: 1, timescale: 30)

    // Export
    let outputFileName = "transformed_\(UUID().uuidString).mp4"
    let outputURL = FileManager.default.temporaryDirectory.appendingPathComponent(outputFileName)

    guard let exportSession = AVAssetExportSession(asset: composition, presetName: AVAssetExportPresetHighestQuality) else {
      rejecter("EXPORT_ERROR", "Failed to create export session", nil)
      return
    }

    exportSession.outputURL = outputURL
    exportSession.outputFileType = .mp4
    exportSession.videoComposition = videoComposition

    exportSession.exportAsynchronously {
      switch exportSession.status {
      case .completed:
        resolver(outputURL.path)
      case .failed:
        rejecter("EXPORT_FAILED", "Export failed: \(exportSession.error?.localizedDescription ?? "Unknown error")", exportSession.error)
      case .cancelled:
        rejecter("EXPORT_CANCELLED", "Export was cancelled", nil)
      default:
        rejecter("EXPORT_ERROR", "Export failed with unknown status", nil)
      }
    }
  }

  @objc
  func cropVideo(_ inputPath: String, aspectRatio: String, position: String, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {

    guard let inputURL = URL(string: inputPath.hasPrefix("file://") ? inputPath : "file://\(inputPath)") else {
      rejecter("INVALID_PATH", "Invalid input path", nil)
      return
    }

    let asset = AVAsset(url: inputURL)

    guard let videoTrack = asset.tracks(withMediaType: .video).first else {
      rejecter("NO_VIDEO_TRACK", "No video track found", nil)
      return
    }

    // Parse aspect ratio
    let components = aspectRatio.split(separator: ":")
    guard components.count == 2,
          let widthRatio = Double(components[0]),
          let heightRatio = Double(components[1]) else {
      rejecter("INVALID_ASPECT_RATIO", "Invalid aspect ratio format. Use format like '16:9'", nil)
      return
    }

    let targetAspectRatio = widthRatio / heightRatio

    // Create composition
    let composition = AVMutableComposition()
    guard let compositionVideoTrack = composition.addMutableTrack(withMediaType: .video, preferredTrackID: kCMPersistentTrackID_Invalid) else {
      rejecter("COMPOSITION_ERROR", "Failed to create composition track", nil)
      return
    }

    do {
      try compositionVideoTrack.insertTimeRange(CMTimeRange(start: .zero, duration: asset.duration), of: videoTrack, at: .zero)
    } catch {
      rejecter("INSERT_ERROR", "Failed to insert time range: \(error.localizedDescription)", nil)
      return
    }

    // Add audio track if exists
    if let audioTrack = asset.tracks(withMediaType: .audio).first {
      if let compositionAudioTrack = composition.addMutableTrack(withMediaType: .audio, preferredTrackID: kCMPersistentTrackID_Invalid) {
        try? compositionAudioTrack.insertTimeRange(CMTimeRange(start: .zero, duration: asset.duration), of: audioTrack, at: .zero)
      }
    }

    // Get video dimensions accounting for transform
    let naturalSize = videoTrack.naturalSize
    let originalTransform = videoTrack.preferredTransform

    // Check if video is rotated 90 or 270 degrees
    let angle = atan2(originalTransform.b, originalTransform.a)
    let isRotated = abs(angle - .pi / 2) < 0.01 || abs(angle + .pi / 2) < 0.01

    // Calculate display dimensions (after transform is applied)
    var displayWidth = naturalSize.width
    var displayHeight = naturalSize.height
    if isRotated {
      swap(&displayWidth, &displayHeight)
    }

    let displayAspectRatio = displayWidth / displayHeight

    // Calculate crop dimensions in display space
    var cropDisplayWidth: CGFloat
    var cropDisplayHeight: CGFloat

    if targetAspectRatio > displayAspectRatio {
      // Target is wider than video - crop height (keep full width)
      cropDisplayWidth = displayWidth
      cropDisplayHeight = displayWidth / CGFloat(targetAspectRatio)
    } else {
      // Target is taller than video - crop width (keep full height)
      cropDisplayHeight = displayHeight
      cropDisplayWidth = displayHeight * CGFloat(targetAspectRatio)
    }

    // Convert crop dimensions back to natural coordinate space
    var cropWidth: CGFloat
    var cropHeight: CGFloat
    if isRotated {
      // When rotated, display width becomes natural height and vice versa
      cropWidth = cropDisplayHeight
      cropHeight = cropDisplayWidth
    } else {
      cropWidth = cropDisplayWidth
      cropHeight = cropDisplayHeight
    }

    // Calculate crop origin based on position
    var cropX: CGFloat = 0
    var cropY: CGFloat = 0

    switch position.lowercased() {
    case "center":
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = (naturalSize.height - cropHeight) / 2
    case "top":
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = 0
    case "bottom":
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = naturalSize.height - cropHeight
    case "left":
      cropX = 0
      cropY = (naturalSize.height - cropHeight) / 2
    case "right":
      cropX = naturalSize.width - cropWidth
      cropY = (naturalSize.height - cropHeight) / 2
    case "top-left":
      cropX = 0
      cropY = 0
    case "top-right":
      cropX = naturalSize.width - cropWidth
      cropY = 0
    case "bottom-left":
      cropX = 0
      cropY = naturalSize.height - cropHeight
    case "bottom-right":
      cropX = naturalSize.width - cropWidth
      cropY = naturalSize.height - cropHeight
    default:
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = (naturalSize.height - cropHeight) / 2
    }

    let cropRect = CGRect(x: cropX, y: cropY, width: cropWidth, height: cropHeight)

    // Set render size to cropped dimensions in display space
    let renderSize = CGSize(width: cropDisplayWidth, height: cropDisplayHeight)

    // Create video composition with crop
    let videoComposition = AVMutableVideoComposition()
    let instruction = AVMutableVideoCompositionInstruction()
    instruction.timeRange = CMTimeRange(start: .zero, duration: asset.duration)

    let layerInstruction = AVMutableVideoCompositionLayerInstruction(assetTrack: compositionVideoTrack)

    // Calculate scale and translation to crop
    // Instead of using cropRectangle, we'll use a transform to position and scale the video
    let scaleX = renderSize.width / cropWidth
    let scaleY = renderSize.height / cropHeight

    // Create transform that scales up and translates to show only the cropped area
    var cropTransform = CGAffineTransform.identity
    cropTransform = cropTransform.translatedBy(x: -cropX, y: -cropY)
    cropTransform = cropTransform.scaledBy(x: scaleX, y: scaleY)

    // Combine with original transform
    let finalTransform = originalTransform.concatenating(cropTransform)

    layerInstruction.setTransform(finalTransform, at: .zero)

    instruction.layerInstructions = [layerInstruction]
    videoComposition.instructions = [instruction]

    videoComposition.renderSize = renderSize
    videoComposition.frameDuration = CMTime(value: 1, timescale: 30)

    // Export
    let outputFileName = "transformed_\(UUID().uuidString).mp4"
    let outputURL = FileManager.default.temporaryDirectory.appendingPathComponent(outputFileName)

    guard let exportSession = AVAssetExportSession(asset: composition, presetName: AVAssetExportPresetHighestQuality) else {
      rejecter("EXPORT_ERROR", "Failed to create export session", nil)
      return
    }

    exportSession.outputURL = outputURL
    exportSession.outputFileType = .mp4
    exportSession.videoComposition = videoComposition

    exportSession.exportAsynchronously {
      switch exportSession.status {
      case .completed:
        resolver(outputURL.path)
      case .failed:
        rejecter("EXPORT_FAILED", "Export failed: \(exportSession.error?.localizedDescription ?? "Unknown error")", exportSession.error)
      case .cancelled:
        rejecter("EXPORT_CANCELLED", "Export was cancelled", nil)
      default:
        rejecter("EXPORT_ERROR", "Export failed with unknown status", nil)
      }
    }
  }

  @objc
  func cropAndRotateVideo(_ inputPath: String, aspectRatio: String, position: String, angle: Int, resolver: @escaping RCTPromiseResolveBlock, rejecter: @escaping RCTPromiseRejectBlock) {

    guard let inputURL = URL(string: inputPath.hasPrefix("file://") ? inputPath : "file://\(inputPath)") else {
      rejecter("INVALID_PATH", "Invalid input path", nil)
      return
    }

    let asset = AVAsset(url: inputURL)

    guard let videoTrack = asset.tracks(withMediaType: .video).first else {
      rejecter("NO_VIDEO_TRACK", "No video track found", nil)
      return
    }

    // Parse aspect ratio
    let components = aspectRatio.split(separator: ":")
    guard components.count == 2,
          let widthRatio = Double(components[0]),
          let heightRatio = Double(components[1]) else {
      rejecter("INVALID_ASPECT_RATIO", "Invalid aspect ratio format. Use format like '16:9'", nil)
      return
    }

    let targetAspectRatio = widthRatio / heightRatio

    // Create composition
    let composition = AVMutableComposition()
    guard let compositionVideoTrack = composition.addMutableTrack(withMediaType: .video, preferredTrackID: kCMPersistentTrackID_Invalid) else {
      rejecter("COMPOSITION_ERROR", "Failed to create composition track", nil)
      return
    }

    do {
      try compositionVideoTrack.insertTimeRange(CMTimeRange(start: .zero, duration: asset.duration), of: videoTrack, at: .zero)
    } catch {
      rejecter("INSERT_ERROR", "Failed to insert time range: \(error.localizedDescription)", nil)
      return
    }

    // Add audio track if exists
    if let audioTrack = asset.tracks(withMediaType: .audio).first {
      if let compositionAudioTrack = composition.addMutableTrack(withMediaType: .audio, preferredTrackID: kCMPersistentTrackID_Invalid) {
        try? compositionAudioTrack.insertTimeRange(CMTimeRange(start: .zero, duration: asset.duration), of: audioTrack, at: .zero)
      }
    }

    // Get video dimensions
    let naturalSize = videoTrack.naturalSize
    let transform = videoTrack.preferredTransform
    let angle_transform = atan2(transform.b, transform.a)
    let isRotated = abs(angle_transform - .pi / 2) < 0.01 || abs(angle_transform + .pi / 2) < 0.01

    // Calculate display dimensions (after original transform is applied)
    var displayWidth = naturalSize.width
    var displayHeight = naturalSize.height
    if isRotated {
      swap(&displayWidth, &displayHeight)
    }

    // Calculate crop dimensions in display space
    var cropDisplayWidth: CGFloat
    var cropDisplayHeight: CGFloat

    if targetAspectRatio > displayWidth / displayHeight {
      cropDisplayWidth = displayWidth
      cropDisplayHeight = displayWidth / CGFloat(targetAspectRatio)
    } else {
      cropDisplayHeight = displayHeight
      cropDisplayWidth = displayHeight * CGFloat(targetAspectRatio)
    }

    // Convert crop dimensions back to natural coordinate space
    var cropWidth: CGFloat
    var cropHeight: CGFloat
    if isRotated {
      cropWidth = cropDisplayHeight
      cropHeight = cropDisplayWidth
    } else {
      cropWidth = cropDisplayWidth
      cropHeight = cropDisplayHeight
    }

    // Calculate crop origin based on position
    var cropX: CGFloat = 0
    var cropY: CGFloat = 0

    switch position.lowercased() {
    case "center":
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = (naturalSize.height - cropHeight) / 2
    case "top":
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = 0
    case "bottom":
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = naturalSize.height - cropHeight
    default:
      cropX = (naturalSize.width - cropWidth) / 2
      cropY = (naturalSize.height - cropHeight) / 2
    }

    let cropRect = CGRect(x: cropX, y: cropY, width: cropWidth, height: cropHeight)

    // Now apply rotation transform
    var rotationTransform: CGAffineTransform
    var finalRenderSize: CGSize

    if angle == 90 {
      // Rotate 90 degrees clockwise AFTER crop
      // The dimensions we're rotating are the cropped dimensions in display space
      rotationTransform = CGAffineTransform(translationX: cropDisplayHeight, y: 0)
      rotationTransform = rotationTransform.rotated(by: .pi / 2)
      finalRenderSize = CGSize(width: cropDisplayHeight, height: cropDisplayWidth)
    } else if angle == -90 || angle == 270 {
      rotationTransform = CGAffineTransform(translationX: 0, y: cropDisplayWidth)
      rotationTransform = rotationTransform.rotated(by: -.pi / 2)
      finalRenderSize = CGSize(width: cropDisplayHeight, height: cropDisplayWidth)
    } else if angle == 180 {
      rotationTransform = CGAffineTransform(translationX: cropDisplayWidth, y: cropDisplayHeight)
      rotationTransform = rotationTransform.rotated(by: .pi)
      finalRenderSize = CGSize(width: cropDisplayWidth, height: cropDisplayHeight)
    } else {
      rotationTransform = .identity
      finalRenderSize = CGSize(width: cropDisplayWidth, height: cropDisplayHeight)
    }

    // Create video composition with BOTH crop and rotation
    let videoComposition = AVMutableVideoComposition()
    let instruction = AVMutableVideoCompositionInstruction()
    instruction.timeRange = CMTimeRange(start: .zero, duration: asset.duration)

    let layerInstruction = AVMutableVideoCompositionLayerInstruction(assetTrack: compositionVideoTrack)

    // First apply original transform (if any), then crop, then rotation
    let combinedTransform = transform.concatenating(rotationTransform)
    layerInstruction.setTransform(combinedTransform, at: .zero)
    layerInstruction.setCropRectangle(cropRect, at: .zero)

    instruction.layerInstructions = [layerInstruction]
    videoComposition.instructions = [instruction]

    videoComposition.renderSize = finalRenderSize
    videoComposition.frameDuration = CMTime(value: 1, timescale: 30)

    // Export
    let outputFileName = "transformed_\(UUID().uuidString).mp4"
    let outputURL = FileManager.default.temporaryDirectory.appendingPathComponent(outputFileName)

    guard let exportSession = AVAssetExportSession(asset: composition, presetName: AVAssetExportPresetHighestQuality) else {
      rejecter("EXPORT_ERROR", "Failed to create export session", nil)
      return
    }

    exportSession.outputURL = outputURL
    exportSession.outputFileType = .mp4
    exportSession.videoComposition = videoComposition

    exportSession.exportAsynchronously {
      switch exportSession.status {
      case .completed:
        resolver(outputURL.path)
      case .failed:
        rejecter("EXPORT_FAILED", "Export failed: \(exportSession.error?.localizedDescription ?? "Unknown error")", exportSession.error)
      case .cancelled:
        rejecter("EXPORT_CANCELLED", "Export was cancelled", nil)
      default:
        rejecter("EXPORT_ERROR", "Export failed with unknown status", nil)
      }
    }
  }

  @objc
  static func requiresMainQueueSetup() -> Bool {
    return false
  }
}
