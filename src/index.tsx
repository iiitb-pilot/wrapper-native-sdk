import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-nprime-face' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

// Protects against silent crashes if the Native Module isn't linked properlys
const NprFaceModule = NativeModules.NprFaceModule
  ? NativeModules.NprFaceModule
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

/**
 * Initializes the SDK. 
 * Accepts a config object from Inji but ignores it to call the 0-arg Java method.
 */
export async function configure(config?: any): Promise<boolean> {
  try {
    const initStatus = await NprFaceModule.configure();
    return initStatus; 
  } catch (e) {
    console.error('NPrime init failed', e);
    return false;
  }
}

/**
 * Performs Face Capture and Comparison.
 * Hardcoded to use the Front Camera.
 */
export async function faceCompare(
  rearCamera: boolean, // Parameter kept for compatibility with Inji calls
  liveness: boolean,
  vcImage: string
): Promise<boolean> {
  try {
    // --- STEP 1: Capture the Face ---
    // 👇 FIX: We pass 'false' instead of 'rearCamera' to force Front Camera (ID 1)
    const capturedTemplate = await NprFaceModule.captureFace(
      false, 
      liveness,
      1 // 1 - GUIDED CAPTURE
    );

    if (!capturedTemplate || capturedTemplate === '') {
      console.error('NPrime Face capture failed or was cancelled');
      return false;
    }

    // --- STEP 2: Compare with VC Image ---
    const status = await NprFaceModule.generateAndIdentifyTemplates(
      capturedTemplate,
      vcImage
    );
    
    return status;
  } catch (e) {
    console.error('NPrime Face comparison failed', e);
    return false;
  }
}