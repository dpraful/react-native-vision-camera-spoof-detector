# Contributing to react-native-vision-camera-spoof-detector

First off, thank you for considering contributing to **react-native-vision-camera-spoof-detector**! It's people like you that make this library such a great tool.

## Code of Conduct

This project and everyone participating in it is governed by our [Code of Conduct](./CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the issue list as you might find out that you don't need to create one. When you are creating a bug report, please include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps which reproduce the problem** in as many details as possible
- **Provide specific examples to demonstrate the steps**
- **Describe the behavior you observed after following the steps**
- **Explain which behavior you expected to see instead and why**
- **Include screenshots and animated GIFs** if possible
- **Include your environment details**:
  - React Native version
  - Device/OS version
  - Library version
  - Any relevant plugin versions

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, please include:

- **Use a clear and descriptive title**
- **Provide a step-by-step description of the suggested enhancement**
- **Provide specific examples to demonstrate the steps**
- **Describe the current behavior** and **the expected behavior**
- **Explain why this enhancement would be useful**

### Pull Requests

- Fill in the required template
- Follow the TypeScript/JavaScript styleguides
- End all files with a newline
- Avoid platform-dependent code

## Development Setup

### Prerequisites

- Node.js >= 16.0.0
- npm >= 8.0.0 or yarn
- React Native >= 0.60.0
- Android SDK (for Android development)
- Xcode (for iOS development, when available)

### Installation

1. **Fork the repository**

```bash
git clone https://github.com/YOUR_USERNAME/react-native-vision-camera-spoof-detector.git
cd react-native-vision-camera-spoof-detector
```

2. **Install dependencies**

```bash
npm install
# or
yarn install
```

3. **Install peer dependencies**

```bash
npm install react-native-vision-camera react-native-reanimated react-native-worklets-core
```

4. **Set up development environment**

For Android development, ensure your Android SDK is properly configured.

### Building and Testing

```bash
# Build the project
npm run build

# Run type checking
npm run type-check

# Run linter
npm run lint

# Run tests
npm run test

# Watch mode for development
npm run dev
```

## Styleguides

### Git Commit Messages

- Use the present tense ("Add feature" not "Added feature")
- Use the imperative mood ("Move cursor to..." not "Moves cursor to...")
- Limit the first line to 72 characters or less
- Reference issues and pull requests liberally after the first line
- Consider starting the commit message with an applicable emoji:
  - 🎨 `:art:` - Improving structure/format
  - 🐛 `:bug:` - Bug fix
  - ⚡ `:zap:` - Performance improvement
  - 📚 `:books:` - Documentation
  - ✅ `:white_check_mark:` - Tests
  - 🔧 `:wrench:` - Configuration
  - 🚀 `:rocket:` - New feature
  - 🔒 `:lock:` - Security
  - ♻️ `:recycle:` - Refactoring

### JavaScript/TypeScript Styleguide

All code must adhere to the following standards:

- Use TypeScript for all new code
- Write clear, self-documenting code
- Use meaningful variable and function names
- Add JSDoc comments for public APIs
- Follow ESLint configuration
- Maximum line length: 100 characters
- Use semicolons
- Use single quotes for strings
- Use 2 spaces for indentation

#### Example:

```typescript
/**
 * Detects if face is centered in frame
 * @param faceRect - Face rectangle bounds
 * @param frameWidth - Frame width in pixels
 * @param frameHeight - Frame height in pixels
 * @returns True if face is centered
 */
export const isFaceCentered = (
  faceRect: FaceRect,
  frameWidth: number,
  frameHeight: number
): boolean => {
  const faceCenterX = faceRect.x + faceRect.width / 2;
  const faceCenterY = faceRect.y + faceRect.height / 2;
  const frameCenterX = frameWidth / 2;
  const frameCenterY = frameHeight / 2;

  return (
    Math.abs(faceCenterX - frameCenterX) <= frameWidth * 0.2 &&
    Math.abs(faceCenterY - frameCenterY) <= frameHeight * 0.15
  );
};
```

### Documentation Styleguide

- Use Markdown
- Reference functions/classes/methods with backticks: `functionName()`
- Use code blocks with language identifiers: \`\`\`javascript
- Include examples where applicable
- Keep paragraphs short and scannable

## Testing

### Writing Tests

All code changes should include corresponding tests:

```typescript
describe('isFaceCentered', () => {
  it('should return true when face is centered', () => {
    const faceRect = { x: 75, y: 50, width: 50, height: 50 };
    const result = isFaceCentered(faceRect, 200, 200);
    expect(result).toBe(true);
  });

  it('should return false when face is off-center', () => {
    const faceRect = { x: 10, y: 10, width: 50, height: 50 };
    const result = isFaceCentered(faceRect, 200, 200);
    expect(result).toBe(false);
  });
});
```

### Running Tests

```bash
npm run test

# Watch mode
npm run test:watch

# Coverage
npm run test:coverage
```

## Documentation Updates

If your changes affect the documented behavior, please update the README or relevant documentation files. This includes:

- New APIs
- Changed behavior
- New examples
- Configuration options

## Performance Considerations

When adding new features, consider:

- Impact on frame processing performance
- Memory usage and garbage collection
- GPU vs CPU acceleration trade-offs
- Compatibility with older devices

## Accessibility

- Ensure code is accessible to developers with different levels of experience
- Write clear comments for complex logic
- Provide examples for new features

## Community

- Use inclusive language
- Be respectful and professional
- Help other contributors when possible
- Share knowledge and experience

## Additional Notes

### Code Review Process

1. Automated tests must pass
2. Code must follow style guidelines
3. Documentation must be updated
4. At least one maintainer review required
5. All conversations must be professional

### Priorities

When choosing what to work on:

1. **High Priority**: Bug fixes, security issues
2. **Medium Priority**: Feature enhancements, performance improvements
3. **Low Priority**: Documentation, examples, refactoring

## License

By contributing, you agree that your contributions will be licensed under its JESCON TECHNOLOGIES PVT LTD License.

## Recognition

Contributors will be recognized in:

- README.md contributors section
- CHANGELOG.md release notes
- GitHub contributors page

## Questions?

- 📧 Email: jescontechnologies@gmail.com
- 💬 [GitHub Issues](https://github.com/dpraful/react-native-vision-camera-spoof-detector/issues)
- 📝 [GitHub Discussions](https://github.com/dpraful/react-native-vision-camera-spoof-detector/discussions)

## Additional Resources

- [React Native Documentation](https://reactnative.dev/)
- [Vision Camera Documentation](https://react-native-vision-camera.com/)
- [TensorFlow Lite Guide](https://www.tensorflow.org/lite)
- [TypeScript Handbook](https://www.typescriptlang.org/docs/)

---

Thank you for contributing! 🙏

Made with ❤️ by JESCON TECHNOLOGIES PVT LTD
