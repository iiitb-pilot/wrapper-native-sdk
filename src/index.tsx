import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-nprime-face' doesn't seem to be linked. \n\n` +
  Platform.select({ ios: "- run 'pod install'\n", default: '' }) +
  '- Rebuild the app\n';


const NprFaceModule = NativeModules.NprFaceModule
  ? NativeModules.NprFaceModule
  : new Proxy({}, { get() { throw new Error(LINKING_ERROR); } });


export function configure(...args: any[]): Promise<boolean> {
  if (args.length === 0 || args[0] === undefined) {
    console.warn("[NPrime] Skipping init: No arguments provided.");
    return Promise.resolve(false);
  }

  try {
    
    return NprFaceModule.configure(...args);
  } catch (err) {
    console.error("[NPrime] Native configuration failed", err);
    return Promise.resolve(false);
  }
}


export async function faceCompare(
  cameraSwitch: boolean,
  livenessSwitch: boolean,
  vcImage: string,
  cameraMode: number = 1
): Promise<boolean> {
  try {
   
    const capturedTemplate = await NprFaceModule.captureFace(cameraSwitch, livenessSwitch, cameraMode);
    
    if (!capturedTemplate) {
      console.log("[NPrime] Capture returned null or empty.");
      return false;
    }

    
    return await NprFaceModule.generateAndIdentifyTemplates(capturedTemplate, vcImage);
  } catch (error) {
    console.error("Face compare error:", error);
    return false;
  }
}