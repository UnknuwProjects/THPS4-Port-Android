# THPS4-Port-Android

[English](README.md) · [Bahasa Indonesia](README.id.md) · [Español](README.es.md) · **Português**

Port não oficial de **Tony Hawk's Pro Skater 4** (Xbox original, versão USA,
title ID `41560017` / AV-023) para Android. O código x86 do Xbox é recompilado
de forma estática para C++ com o
[XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp) e compilado para
arm64: sem emulador, o jogo roda como código nativo no celular.

> [!NOTE]
> Este projeto não inclui nenhum arquivo do jogo. Você precisa dos arquivos da
> sua própria cópia do jogo, adquirida legalmente (o `default.xbe` / a imagem
> de disco da versão USA), para compilar ou jogar.

## Status

Em desenvolvimento ativo. O jogo abre e pode ser jogado do início ao fim em
hardware real, e há builds públicas no
[repositório de lançamentos](https://github.com/UnknuwProjects/THPS4-Android-Releases).
O app Android e as ferramentas do projeto já estão aqui; o código nativo —
o jogo recompilado e a camada que o liga — chega com a versão **1.0.4**.

## Como funciona

- **Recompilação estática**: o XBE é traduzido para C++ antes da compilação e
  compilado para arm64-v8a. Nada é interpretado em tempo de execução.
- **Suporte ao XDK 4928**: o THPS4 foi feito com o XDK 4928, que o
  XboxXBE-Recomp não cobria. Este projeto fez esse bootstrap: as assinaturas
  das funções e o layout do dispositivo foram recuperados do próprio jogo e
  verificados campo a campo.
- **Renderização**: uma camada D3D8 reimplementada sobre o Vulkan 1.1, com
  drivers de GPU personalizados opcionais (ex.: Turnip no Adreno).
- **O port vive dentro do jogo**: os ajustes gráficos, o idioma e os créditos
  ficam no menu Options do próprio jogo como *Ajustes do Port*, em quatro
  idiomas: inglês, Bahasa Indonesia, espanhol e português.
- **Jogo online**: o modo local de 2 jogadores e o System Link funcionam pela
  internet por meio de um pequeno servidor da comunidade, com alternativa via
  Wi-Fi quando nenhum servidor está acessível.
- **Extras**: controles por toque e gamepad com remapeamento, pacotes de mods,
  trilhas sonoras personalizadas a partir dos seus próprios arquivos WMA e um
  preset para aparelhos modestos com resolução dinâmica.

## O app

| | |
|---|---|
| Nome | **THPS4** |
| Pacote | `com.unknuw.thps4.unofficial` |
| Android | 10–16 (minSdk 29, targetSdk 36), arm64-v8a |

## Builds oficiais

Os APKs oficiais são assinados com um único certificado. Confira antes de
confiar em uma build que se apresente como este port:

```
SHA-256: 12474f52e9b6a461bceb2af6882a0543d8dc6e49fc8fcb06ac25de83489885bb
```

O jogo também verifica a própria assinatura ao iniciar e se recusa a rodar se
ela não bater.

## Créditos

- [XboxXBE-Recomp](https://github.com/TGP482/XboxXBE-Recomp), de [**TGP482**](https://github.com/TGP482): o
  recompilador e o runtime SDK sobre os quais este port foi construído
- [XboxXBERecomp](https://github.com/UnknuwProjects/XboxXBERecomp), de
  [**UNKNUW**](https://github.com/UnknuwProjects): o fork deste
  projeto, com suporte ao XDK 4928 e as alterações de runtime de que
  este port precisa
- [XbSymbolDatabase](https://github.com/Cxbx-Reloaded/XbSymbolDatabase)
  (Cxbx-Reloaded): padrões OOVPA usados na verificação cruzada
- [SDL3](https://github.com/libsdl-org/SDL): janelas, entrada e áudio no Android
- [playit.gg](https://playit.gg): o túnel que hospeda o servidor da comunidade

Apoie o projeto:
[Sociabuzz](https://sociabuzz.com/unknuw/tribe) ·
[Saweria](https://saweria.co/Unknuw)

Encontrou um bug? Abra uma issue neste repositório.

## Licença

Este projeto é licenciado sob a **GNU General Public License v3.0** (GPL-3.0);
veja [LICENSE](LICENSE). O SDK do XboxXBE-Recomp incluído mantém a própria
licença.

*Projeto de fã não oficial. Sem afiliação com a Activision ou a Neversoft.
Tony Hawk's Pro Skater 4 é uma marca de seus respectivos proprietários.*
