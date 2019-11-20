# Quboo CLI

This is a Command Line Interface to assign score to players in Quboo.

If you landed here without knowing Quboo, please visit first the [Quboo Documentation Site](https://quboo.tpd.io) to get the full context about this tool.

## Usage

The CLI tool connects to the Quboo API with your Access and Secret Keys. Therefore, please make sure you set the environment variables `QUBOO_ACCESS_KEY` and `QUBOO_SECRET_KEY` accordingly. You can get these values from your Quboo's administrator page, section 'Settings'.

Since this tool is mainly intended to be executed from a container, you need Docker if you want run it from your computer.

The basic usage is as follows:

```bash
docker run -it -e QUBOO_ACCESS_KEY -e QUBOO_SECRET_KEY -e QUBOO_PLAYER_USERNAME=[player-alias] mechero/quboo-cli:latest quboo release|documentation|(number) "[description]"
```

As you see, this command line assumes you have set both keys (access and secret) already as environment variables, therefore you don't need to set them in the command but only reference.

For example, you can give score to the user with login `johndoe` following the Release use case in the Quboo docs.

```bash
docker run -it -e QUBOO_ACCESS_KEY -e QUBOO_SECRET_KEY -e QUBOO_PLAYER_USERNAME=johndoe mechero/quboo-cli:latest quboo release "Front-end release"
```

Or you could give him 25 points because you designed your own game (generic score):

```bash
docker run -it -e QUBOO_ACCESS_KEY -e QUBOO_SECRET_KEY -e QUBOO_PLAYER_USERNAME=johndoe mechero/quboo-cli:latest quboo 25 "Coaching and pair programming"
```

## Integration with existing CI/CD tools

The real power of this tool is not in running it as a standalone CLI but in integrating it within your CI/CD tooling. That's why this script uses some conventions that will allow automatic player name detection from a running pipeline.
