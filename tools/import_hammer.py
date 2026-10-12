"""Import the owner's Blockbench cuboid glTF without a runtime glTF dependency.

    python tools/import_hammer.py ../hammer/source/model.gltf

The source is read only. Minecraft face order follows refs/src/.../renderer/FaceInfo.java.
"""
import argparse
import base64
import io
import itertools
import json
from pathlib import Path
import shutil
import struct

from PIL import Image


FACE_CORNERS = {
    'down': [(0, 0, 1), (0, 0, 0), (1, 0, 0), (1, 0, 1)],
    'up': [(0, 1, 0), (0, 1, 1), (1, 1, 1), (1, 1, 0)],
    'north': [(1, 1, 0), (1, 0, 0), (0, 0, 0), (0, 1, 0)],
    'south': [(0, 1, 1), (0, 0, 1), (1, 0, 1), (1, 1, 1)],
    'west': [(0, 1, 0), (0, 0, 0), (0, 0, 1), (0, 1, 1)],
    'east': [(1, 1, 1), (1, 0, 1), (1, 0, 0), (1, 1, 0)],
}
NORMALS = {(0, -1, 0): 'down', (0, 1, 0): 'up', (0, 0, -1): 'north',
           (0, 0, 1): 'south', (-1, 0, 0): 'west', (1, 0, 0): 'east'}
PARTS = ['handle', 'head', 'pommel', 'collar']


def face_uv(ordered):
    # A Minecraft UV rectangle may be mirrored, then rotated by a quarter turn.
    us, vs = sorted({uv[0] for uv in ordered}), sorted({uv[1] for uv in ordered})
    assert len(us) == len(vs) == 2, 'Expected rectangular UVs'
    for flip_u, flip_v, rotation in itertools.product([False, True], [False, True], range(4)):
        u0, u1 = us[::-1] if flip_u else us
        v0, v1 = vs[::-1] if flip_v else vs
        corners = [(u0, v0), (u0, v1), (u1, v1), (u1, v0)]
        if all(ordered[i] == corners[(i + rotation) % 4] for i in range(4)):
            result = {'uv': [v * 16 for v in (u0, v0, u1, v1)]}
            if rotation:
                result['rotation'] = rotation * 90
            return result
    raise ValueError('UVs cannot be represented by a Minecraft face')


def convert(source, resources):
    data = json.loads(source.read_text(encoding='utf-8-sig'))
    assert data['asset']['generator'].startswith('Blockbench ')
    assert not data.get('skins') and not data.get('animations') and not data.get('extensionsRequired')
    assert len(data['meshes']) == len(data['images']) == 4
    buffers = [base64.b64decode(b['uri'].split(',', 1)[1]) for b in data['buffers']]
    assert all(len(raw) == b['byteLength'] for raw, b in zip(buffers, data['buffers']))

    def accessor(index):
        a = data['accessors'][index]
        view = data['bufferViews'][a['bufferView']]
        components = {'SCALAR': 1, 'VEC2': 2, 'VEC3': 3}[a['type']]
        fmt = '<' + {5126: 'f', 5123: 'H'}[a['componentType']] * components
        start = view.get('byteOffset', 0) + a.get('byteOffset', 0)
        stride = view.get('byteStride', struct.calcsize(fmt))
        return [struct.unpack_from(fmt, buffers[view['buffer']], start + i * stride) for i in range(a['count'])]

    # Separate PNGs have the original Blockbench orientation; the embedded PNGs are vertically flipped.
    textures = []
    for i, image in enumerate(data['images']):
        original = source.parent.parent / 'textures' / f'gltf_embedded_{i}.png'
        embedded = Image.open(io.BytesIO(base64.b64decode(image['uri'].split(',', 1)[1]))).convert('RGBA')
        with Image.open(original) as png:
            assert embedded.tobytes() == png.convert('RGBA').transpose(Image.Transpose.FLIP_TOP_BOTTOM).tobytes()
        textures.append(original)

    elements = []

    def visit(index, parent_translation=(0, 0, 0)):
        node = data['nodes'][index]
        assert not any(key in node for key in ('rotation', 'scale', 'matrix')), 'Expected unrotated cuboids'
        translation = tuple(a + b for a, b in zip(parent_translation, node.get('translation', (0, 0, 0))))
        if 'mesh' in node:
            primitives = data['meshes'][node['mesh']]['primitives']
            assert len(primitives) == 1
            primitive = primitives[0]
            assert primitive.get('mode', 4) == 4
            positions = accessor(primitive['attributes']['POSITION'])
            normals = accessor(primitive['attributes']['NORMAL'])
            uvs = [(u, 1 - v) for u, v in accessor(primitive['attributes']['TEXCOORD_0'])]
            indices = [v[0] for v in accessor(primitive['indices'])]
            assert len(positions) == 24 and len(indices) == 36 and max(indices) < 24
            low = [min(v[axis] for v in positions) for axis in range(3)]
            high = [max(v[axis] for v in positions) for axis in range(3)]
            faces = {}
            material = data['materials'][primitive['material']]['pbrMetallicRoughness']
            texture = data['textures'][material['baseColorTexture']['index']]['source']
            for normal, direction in NORMALS.items():
                vertices = [i for i, n in enumerate(normals) if n == normal]
                assert len(vertices) == 4
                uv_at = {positions[i]: uvs[i] for i in vertices}
                ordered = [uv_at[tuple((high if bit else low)[axis] for axis, bit in enumerate(corner))]
                           for corner in FACE_CORNERS[direction]]
                faces[direction] = face_uv(ordered) | {'texture': f'#{texture}'}
            elements.append({'from': [(v + t) * 16 for v, t in zip(low, translation)],
                             'to': [(v + t) * 16 for v, t in zip(high, translation)], 'faces': faces})
        for child in node.get('children', []):
            visit(child, translation)

    for node in data['scenes'][data.get('scene', 0)]['nodes']:
        visit(node)
    assert len(elements) == 4
    model = {'credit': 'Imported from the owner-provided Blockbench 4.6.0 glTF with tools/import_hammer.py',
             'ambientocclusion': False,
             'textures': {str(i): f'cinematic:item/prop/hammer_{part}' for i, part in enumerate(PARTS)},
             'elements': elements}
    model['textures']['particle'] = '#1'
    destination = resources / 'models/prop/hammer.json'
    destination.parent.mkdir(parents=True, exist_ok=True)
    (resources / 'textures/item/prop').mkdir(parents=True, exist_ok=True)
    destination.write_text(json.dumps(model, indent=2) + '\n', encoding='utf-8')
    for original, part in zip(textures, PARTS):
        shutil.copyfile(original, resources / f'textures/item/prop/hammer_{part}.png')
    print('Imported four cuboids, 24 faces and four original PNG textures:', destination)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('model', type=Path)
    args = parser.parse_args()
    convert(args.model, Path(__file__).resolve().parents[1] / 'src/main/resources/assets/cinematic')
