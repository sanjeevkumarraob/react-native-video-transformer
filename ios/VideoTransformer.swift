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
  static func requiresMainQueueSetup() -> Bool {
    return false
  }
}
